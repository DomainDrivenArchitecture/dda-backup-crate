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

(ns dda.pallet.dda-backup-crate.infra.lib.backup-lib
  (require
   [schema.core :as s]
   [dda.pallet.dda-backup-crate.infra.schema :as schema]))

(s/defn backup-files-tar
  "bash script part to backup as tgz."
  [backup-name :- s/Str
   backup-transport-folder :- s/Str
   user-name :- s/Str
   backup-element :- schema/BackupElement]
  (let [{:keys [type backup-file-name backup-path]} backup-element
        tar-options (case type
                      :file-compressed "cvzf"
                      :file-plain "cvf")]
    ["#backup the files"
     (str "cd " root-dir)
     (str "tar " tar-options " " backup-transport-folder "/"
          backup-file-name " " subdir-to-save)
     (str "chown " user-name ":" user-name " " backup-transport-folder "/"
          backup-file-name)
     ""]))

(s/defn backup-files-rsync
  "bash script part to backup with rsync."
  [backup-name :- s/Str
   backup-transport-folder :- s/Str
   backup-element :- schema/BackupElement]
  (let [{:keys [backup-file-name root-dir subdir-to-save]} backup-element]
     ["#backup the files"
      (str "cd " root-dir)
      (str "rsync -Aax " subdir-to-save " " backup-transport-folder "/"
           backup-file-name)
      ""]))

(s/defn backup-mysql
  "bash script part to backup a mysql db."
  [backup-name :- s/Str
   backup-transport-folder :- s/Str
   user-name :- s/Str
   backup-element :- schema/BackupElement]
  (let [{:keys [backup-file-name db-user-name db-user-passwd db-name]} backup-element]
     ["#backup db"
      (str "mysqldump --no-create-db=true -h localhost -u " db-user-name
           " -p" db-user-passwd
           " " db-name " > " backup-transport-folder "/"
           backup-file-name)
      (str "chown " user-name ":" user-name " " backup-transport-folder "/"
           backup-file-name)
      ""]))
