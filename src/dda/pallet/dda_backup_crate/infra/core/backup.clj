; Licensed to the Apache Software Foundation (ASF) under one
; or more contributor license agreements. See the NOTICE file
; distributed with this work for additional information
; regarding copyright ownership. The ASF licenses this file
; to you under the Apache License, Version 2.0 (the
; "License"); you may not use this file except in compliance
; with the License. You may obtain a copy of the License at
;
; http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns dda.pallet.dda-backup-crate.infra.core.backup
  (:require
   [schema.core :as s]
   [schema-tools.core :as st]
   [pallet.actions :as actions]
   [pallet.stevedore :as stevedore]
   [dda.pallet.dda-backup-crate.infra.core.backup-element :as backup-element]
   [dda.pallet.dda-backup-crate.infra.lib.common-lib :as common-lib]
   [dda.pallet.dda-backup-crate.infra.lib.backup-lib :as backup-lib]
   [dda.pallet.dda-backup-crate.infra.lib.transport-lib :as transport-lib]
   [dda.pallet.dda-backup-crate.infra.lib.restore-lib :as restore-lib]
   [dda.pallet.dda-backup-crate.infra.duplicity.duplicity :as duplicity]))

(def User
  "User configuration"
  {:name s/Str
   :encrypted-passwd s/Str})

(def ScriptType
  "The backup elements"
  (s/enum :backup :restore :source-transport))

(s/defn ^:always-validate backup-element-lines
  ""
  [backup-name :- s/Str
   element :- backup-element/BackupElement]
  (case (st/get-in element [:type])
    :file-compressed (backup-lib/backup-files-tar backup-name element)
    :mysql (backup-lib/backup-mysql backup-name element)
    :duplicity (backup-lib/backup-files-duplicity element)))

(defn backup-script-lines
  "create the backup script for defined elements."
  [config]
  (let [service-restart (get-in config [:service-restart])
        backup-name (get-in config [:backup-name])]
    (into
     []
     (concat
      common-lib/head
      common-lib/export-timestamp
      (when (contains? config :service-restart)
        (common-lib/stop-app-server service-restart))
      (mapcat #(backup-element-lines backup-name %)
              (get-in config [:elements]))
      (when (contains? config :service-restart)
        (common-lib/start-app-server service-restart))))))

(s/defn transport-element-lines
  ""
  [backup-name :- s/Str
   gens-stored-on-source-system :- s/Num
   element :- backup-element/BackupElement]
  (let [file-name-pattern
        (str (backup-element/backup-file-prefix backup-name element) "_*")]
    [(str "  (ls -t " file-name-pattern "|head -n " gens-stored-on-source-system
          ";ls " file-name-pattern ")|sort|uniq -u|xargs rm -r")]))

(defn transport-script-lines
  "create the transportation script"
  [config]
  (let [backup-name (get-in config [:backup-name])
        gens-stored-on-source-system (get-in config [:gens-stored-on-source-system])]
    (into
     []
     (concat
      common-lib/head
      transport-lib/pwd-test
      (mapcat #(transport-element-lines backup-name gens-stored-on-source-system %)
              (st/get-in config [:elements]))
      ["fi"
       ""]))))

(s/defn restore-element-lines
  ""
  [element :- backup-element/BackupElement]
  (case (st/get-in element [:type])
    :file-compressed (restore-lib/restore-tar-script element)
    :mysql (restore-lib/restore-mysql-script element)
    :duplicity (restore-lib/restore-duplicity element)))

(defn restore-script-lines
  "create the restore script"
  [config]
  (let [service-restart (get-in config [:service-restart])
        backup-name (get-in config [:backup-name])
        elements (get-in config [:elements])
        dup (duplicity/check-for-dup config)]
    (if dup
      (into
       []
       (concat
        common-lib/head
        (when (contains? config :service-restart)
          (common-lib/stop-app-server service-restart))
        (mapcat restore-element-lines elements)
       (when (contains? config :service-restart)
         (common-lib/start-app-server service-restart))))
      (into
       []
       (concat
        common-lib/head
        restore-lib/restore-parameters
        restore-lib/restore-navigate-to-restore-location
        (restore-lib/provide-restore-dumps elements)
        (restore-lib/restore-head-script elements)
        (when (contains? config :service-restart)
          (common-lib/stop-app-server service-restart))
        (mapcat restore-element-lines elements)
        restore-lib/restore-tail)))))

(s/defn write-backup-file
  "Write the backup file."
  [config
   script-type :- ScriptType]
  (let [cron-name (str (get-in config [:backup-name]) "_" (name script-type))
        script-name (str cron-name ".sh")
        script-path (str (get-in config [:script-path]) script-name)
        script-lines (case script-type
                       :backup (backup-script-lines config)
                       :restore (restore-script-lines config)
                       :source-transport (transport-script-lines config))
        cron-order (case script-type
                     :backup "10_"
                     :source-transport "20_"
                     "")]
    (actions/remote-file
     script-path
     :mode "700"
     :overwrite-changes true
     :literal true
     :content (clojure.string/join
               \newline
               script-lines))
    (when (not= script-type :restore)
      (actions/symbolic-link
       script-path
       (str "/etc/cron.daily/" cron-order cron-name)
       :action :create))))

(s/defn create-backup-directory
  "create the backup user with directory structure."
  [user :- User]
  (let [backup-user-name (st/get-in user [:name])]
    (actions/directory (str "/home/" backup-user-name "/transport-outgoing")
                       :action :create
                       :owner backup-user-name
                       :group backup-user-name)
    (actions/directory (str "/home/" backup-user-name "/store")
                       :action :create
                       :owner backup-user-name
                       :group backup-user-name)
    (actions/directory (str "/home/" backup-user-name "/restore")
                       :action :create
                       :owner backup-user-name
                       :group backup-user-name)))

(defn create-script-environment
  "create directory for backup scripts."
  [script-path]
  (actions/directory
   script-path
   :action :create
   :owner "root"
   :group "root"))

(defn write-scripts
  "write the backup scripts to script environment"
  [config]
  (write-backup-file config :backup)
  (when (not (duplicity/check-for-dup config))
    (write-backup-file config :source-transport))
  (write-backup-file config :restore))