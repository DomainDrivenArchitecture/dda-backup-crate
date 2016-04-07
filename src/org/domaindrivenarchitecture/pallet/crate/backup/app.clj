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

(ns org.domaindrivenarchitecture.pallet.crate.backup.app
  (:require
    [schema.core :as s]
    [schema-tools.core :as st]
    [pallet.actions :as actions]
    [pallet.stevedore :as stevedore]
    [org.domaindrivenarchitecture.cm.util :as util]
    [org.domaindrivenarchitecture.pallet.crate.backup.backup-element :as backup-element]
    [org.domaindrivenarchitecture.pallet.crate.backup.common-lib :as common-lib]
    [org.domaindrivenarchitecture.pallet.crate.backup.backup-lib :as backup-lib]
    [org.domaindrivenarchitecture.pallet.crate.backup.transport-lib :as transport-lib]
    [org.domaindrivenarchitecture.pallet.crate.backup.restore-lib :as restore-lib]
    ))

(def User
  "User configuration"
  {:name s/Str
   :encrypted-passwd s/Str})

(def ScriptType
  "The backup elements"
  (s/enum :backup :restore :source-transport))

(s/defn backup-element-lines
  ""
  [backup-name :- s/Str
   element :- backup-element/BackupElement]
  (case (st/get-in element [:type])
    :file-compressed (backup-lib/backup-files-tar backup-name element)
    :mysql (backup-lib/backup-mysql backup-name element)
    ))

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
        (common-lib/stop-app-server service-restart)
        (mapcat #(backup-element-lines backup-name %)
             (get-in config [:elements]))
        (common-lib/start-app-server service-restart)
        ))))

(s/defn transport-element-lines
  ""
  [backup-name :- s/Str 
   gens-stored-on-source-system :- s/Num  
   element :- backup-element/BackupElement]
  (let [file-name-pattern 
        (str (backup-element/backup-file-prefix backup-name element) "_*")]
    [(str "  (ls -t " file-name-pattern "|head -n " gens-stored-on-source-system
           ";ls " file-name-pattern")|sort|uniq -u|xargs rm -r")]
    ))

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
         ""]
        ))
    ))

(s/defn restore-element-lines
  ""
  [element :- backup-element/BackupElement]
  (case (st/get-in element [:type])
    :file-compressed (restore-lib/restore-tar-script element)
    :mysql (restore-lib/restore-mysql-script element)
    ))

(defn restore-script-lines
  "create the restore script"
  [config]
  (let [service-restart (get-in config [:service-restart])
        backup-name (get-in config [:backup-name])
        elements (get-in config [:elements])]
    (into 
      [] 
      (concat 
        common-lib/head
        restore-lib/restore-parameters
        restore-lib/restore-navigate-to-restore-location
        (restore-lib/provide-restore-dumps elements)
        (restore-lib/restore-head-script elements)
        (common-lib/stop-app-server service-restart)
        (mapcat restore-element-lines elements)
        restore-lib/restore-tail
        ))
  ))

(s/defn write-backup-file
  "Write the backup file."
  [config 
   script-type :- ScriptType]
  (let [cron-name (str (get-in config [:script-path]) (get-in config [:backup-name]) "_" (name script-type))
        script-name (str cron-name ".sh")
        script-lines (case script-type
                       :backup (backup-script-lines config)
                       :restore (restore-script-lines config)
                       :source-transport (transport-script-lines config))]
  (actions/remote-file
    script-name
    :mode "700"
    :overwrite-changes true
    :literal true
    :content (clojure.string/join
               \newline
               script-lines))
  (when (not= script-type :restore)
    (actions/symbolic-link 
      script-name
      (str "/etc/cron.daily/10_" cron-name)
      :action :create))
  ))


(s/defn create-backup-source-user
  "create the backup user with directory structure."
  [user :- User]
  (let [backup-user-name (st/get-in user [:name])]
    (actions/user backup-user-name 
                  :action :create 
                  :create-home true 
                  :shell :bash
                  :password (st/get-in user [:encrypted-passwd]))
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
                       :group backup-user-name)
    ))

(defn create-script-environment
  "create directory for backup scripts."
  [script-path]
  (actions/directory 
    script-path
    :action :create
    :owner "root"
    :group "root")
  )

(defn write-scripts
  "write the backup scripts to script environment"
  [config]
  (write-backup-file config :backup)
  (write-backup-file config :source-transport)
  (write-backup-file config :restore)
  )