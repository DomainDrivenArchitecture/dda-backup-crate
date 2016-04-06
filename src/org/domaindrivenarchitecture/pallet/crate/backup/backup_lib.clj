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

(ns org.domaindrivenarchitecture.pallet.crate.backup.backup-lib
  (require 
    [schema.core :as s]
    [org.domaindrivenarchitecture.pallet.crate.backup.backup-element :as element]))

(s/defn backup-files-tar
  "bash script part to backup as tgz."
  [backup-name :- s/Str
   element :- element/BackupElement]
  (let [tar-options (case (get-in element [:type])
                      :file-compressed "cvzf"
                      :file-plain "cvf")]
  ["#backup the files" 
   (str "cd " (get-in element [:root-dir]))  
   (str "tar " tar-options " /home/dataBackupSource/transport-outgoing/" 
        (element/backup-file-name backup-name element) " " (get-in element [:subdir-to-save]))
   (str "chown dataBackupSource:dataBackupSource /home/dataBackupSource/transport-outgoing/"
        (element/backup-file-name backup-name element))
   ""])
  )

(s/defn backup-files-rsync
  "bash script part to backup with rsync."
  [backup-name :- s/Str
   element :- element/BackupElement]
  ["#backup the files" 
   (str "cd " (get-in element [:root-dir])) 
   (str "rsync -Aax " (get-in element [:subdir-to-save]) " /home/dataBackupSource/transport-outgoing/" 
        (element/backup-file-name backup-name element))
   ""])

(s/defn backup-mysql
  "bash script part to backup a mysql db."
  [backup-name :- s/Str
   element :- element/BackupElement]
  ["#backup db"
   (str "mysqldump --no-create-db=true -h localhost -u " (get-in element [:db-user-name])
        " -p" (get-in element [:db-user-passwd]) 
        " " (get-in element [:db-name]) " > /home/dataBackupSource/transport-outgoing/"
        (element/backup-file-name backup-name element))
   (str "chown dataBackupSource:dataBackupSource /home/dataBackupSource/transport-outgoing/"
        (element/backup-file-name backup-name element))
   ""]
  )