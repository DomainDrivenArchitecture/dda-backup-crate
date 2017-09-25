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

(ns dda.pallet.dda-backup-crate.infra.backup-elements
  (:require
   [schema.core :as s]
   [schema-tools.core :as st]
   [pallet.actions :as actions]
   [pallet.stevedore :as stevedore]
   [dda.pallet.dda-backup-crate.infra.schema :as schema]
   [dda.pallet.dda-backup-crate.infra.lib.common-lib :as common-lib]
   [dda.pallet.dda-backup-crate.infra.lib.backup-lib :as backup-lib]
   [dda.pallet.dda-backup-crate.infra.lib.transport-lib :as transport-lib]
   [dda.pallet.dda-backup-crate.infra.lib.restore-lib :as restore-lib]
   [dda.pallet.dda-backup-crate.infra.lib.duplicity-lib :as duplicity-lib]))

(s/defn write-file
  "Write the backup file."
   [backup-name :- s/Str
    script-type :- schema/ScriptType
    script-path :- s/Str
    cron-order :- s/Str
    script-lines :- [s/Str]]
  (let [cron-name (str backup-name "_" (name script-type))
        script-name (str cron-name ".sh")
        script-path (str script-path script-name)]
    (actions/remote-file
     script-path
     :mode "554"
     :literal true
     :content (clojure.string/join
               \newline
               script-lines))
    (when (some? cron-order)
      (actions/symbolic-link
       script-path
       (str "/etc/cron.daily/" cron-order cron-name)
       :action :create))))

(s/defn backup-element-lines
  ""
  [backup-name :- s/Str
   backup-store-folder :- s/Str
   user-name :- s/Str
   element :- schema/BackupElement]
  (println element)
  (case (get-in element [:type])
    :file-compressed (backup-lib/backup-files-tar backup-name backup-store-folder user-name element)
    :mysql (backup-lib/backup-mysql backup-name backup-store-folder user-name element)))

(s/defn backup-script-lines
  "create the backup script for defined elements."
  [backup-name :- s/Str
   backup-store-folder :- s/Str
   service-restart :- s/Str
   user-name :- s/Str
   elements :- [schema/BackupElement]]
  (into
   []
   (concat
    common-lib/head
    common-lib/export-timestamp
    (when (some? service-restart)
      (common-lib/stop-app-server service-restart))
    (mapcat #(backup-element-lines backup-name backup-store-folder user-name %) elements)
    (when (some? service-restart)
      (common-lib/start-app-server service-restart)))))

(s/defn transport-element-lines
  ""
  [gens-stored-on-source-system :- s/Num
   element :- schema/BackupElement]
  (let [{:keys [backup-file-prefix-pattern]} element]
    [(str "  (ls -t "  backup-file-prefix-pattern "|head -n " gens-stored-on-source-system
          ";ls "  backup-file-prefix-pattern ")|sort|uniq -u|xargs rm -r")]))

(s/defn transport-script-lines
  "create the transportation script"
  [duplicity? :- s/Bool
   backup-script-path :- s/Str
   backup-transport-folder :- s/Str
   backup-store-folder :- s/Str
   local-management :- [schema/LocalManagement]
   elements :- [schema/BackupElement]]
  (let [{:keys [gens-stored-on-source-system]} local-management]
    (into
     []
     (concat
      common-lib/head
      (when duplicity?
        (duplicity-lib/transport-backup backup-script-path))
      (transport-lib/move-local backup-transport-folder backup-store-folder)
      (mapcat #(transport-element-lines gens-stored-on-source-system %) elements)
      ["fi"
       ""]))))

(s/defn restore-element-lines
  [element :- schema/BackupElement]
  (case (:type element)
    :file-compressed (restore-lib/restore-tar-script element)
    :mysql (restore-lib/restore-mysql-script element)))

(s/defn restore-script-lines
  "create the restore script"
  [duplicity? :- s/Bool
   backup-script-path :- s/Str
   backup-restore-folder :- s/Str
   service-restart :- s/Str
   transport-management :- schema/TransportManagement
   elements :- [schema/BackupElement]]
  (into
   []
   (concat
    common-lib/head
    restore-lib/restore-usage
    (when duplicity?
      (duplicity-lib/transport-restore backup-script-path))
    (restore-lib/restore-navigate-to-restore-location backup-restore-folder)
    (when (contains? transport-management :duplicity-push))
      ;transport duplicity
    (when (contains? transport-management :ssh-pull)
      (restore-lib/provide-restore-dumps elements))
    (restore-lib/restore-head-script elements)
    (when (some? service-restart)
      (common-lib/stop-app-server service-restart))
    (mapcat restore-element-lines elements)
    restore-lib/restore-tail)))
