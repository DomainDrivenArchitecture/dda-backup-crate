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

(ns dda.pallet.dda-backup-crate.infra.script
  (:require
   [schema.core :as s]
   [schema-tools.core :as st]
   [pallet.actions :as actions]
   [pallet.stevedore :as stevedore]
   [dda.pallet.dda-backup-crate.infra.schema :as schema]
   [dda.pallet.dda-backup-crate.infra.lib.common-lib :as common-lib]
   [dda.pallet.dda-backup-crate.infra.lib.backup-lib :as backup-lib]
   [dda.pallet.dda-backup-crate.infra.lib.transport-lib :as transport-lib]
   [dda.pallet.dda-backup-crate.infra.lib.restore-lib :as restore-lib]))



(s/defn transport-element-lines
  ""
  [backup-name :- s/Str
   gens-stored-on-source-system :- s/Num
   element :- schema/BackupElement]
  (let [file-name-pattern
        (str (file-convention/backup-file-prefix backup-name element) "_*")]
    [(str "  (ls -t " file-name-pattern "|head -n " gens-stored-on-source-system
          ";ls " file-name-pattern ")|sort|uniq -u|xargs rm -r")]))

(defn transport-script-lines
  "create the transportation script"
  [config]
  (let [{:keys [backup-name local-management elements]} config
        gens-stored-on-source-system (get-in local-management [:gens-stored-on-source-system])]
    (into
     []
     (concat
      common-lib/head
      transport-lib/pwd-test
      (mapcat #(transport-element-lines backup-name gens-stored-on-source-system %) elements)
      ["fi"
       ""]))))

(s/defn restore-element-lines
  ""
  [element :- schema/BackupElement]
  (case (st/get-in element [:type])
    :file-compressed (restore-lib/restore-tar-script element)
    :mysql (restore-lib/restore-mysql-script element)
    :duplicity (restore-lib/restore-duplicity element)))

(defn restore-script-lines
  "create the restore script"
  [config]
  (let [{:keys [service-restart backup-name  elements transport-management]} config]
    (if (contains? transport-management :duplicity-push)
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
  [config :- schema/BackupConfig
   script-type :- ScriptType]
  (let [{:keys [backup-name script-path]} config
        cron-name (str backup-name "_" (name script-type))
        script-name (str cron-name ".sh")
        script-path (str script-path script-name)
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

(defn write-scripts
  "write the backup scripts to script environment"
  [config]
  (write-backup-file config :backup)
  (write-backup-file config :source-transport)
  (write-backup-file config :restore))
