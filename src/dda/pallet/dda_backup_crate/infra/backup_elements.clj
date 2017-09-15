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
   [dda.pallet.dda-backup-crate.infra.lib.backup-lib :as backup-lib]))

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
     :mode "700"
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
  (case (st/get-in element [:type])
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
