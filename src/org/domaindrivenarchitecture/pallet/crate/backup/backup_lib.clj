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
    [org.domaindrivenarchitecture.pallet.crate.backup.common-lib :as common]))

(defn backup-files-tar
  [& {:keys [root-dir
             subdir-to-save
             app 
             instance-name
             file-type]
      :or {file-type :file-compressed}}]
  (let [tar-options (case file-type
                         :file-compressed "cvzf"
                         :file-plain "cvf")]
  ["#backup the files" 
   (str "cd " root-dir)  
   (str "tar " tar-options " /home/dataBackupSource/transport-outgoing/" 
        (common/backup-file-name app instance-name file-type) " " subdir-to-save)
   (str "chown dataBackupSource:dataBackupSource /home/dataBackupSource/transport-outgoing/"
        (common/backup-file-name app instance-name file-type))
   ""])
  )

(defn backup-files-rsync
  [& {:keys [root-dir
             subdir-to-save
             app 
             instance-name]
      }]
  (let [file-type :rsync]
  ["#backup the files" 
   (str "cd " root-dir) 
   (str "rsync -Aax " subdir-to-save " /home/dataBackupSource/transport-outgoing/" 
        (common/backup-file-name app instance-name file-type))
   ""])
  )

(defn backup-mysql
  ""
  [& {:keys [db-user 
             db-pass 
             db-name 
             app 
             instance-name]
      }]
  ["#backup db"
   (str "mysqldump --no-create-db=true -h localhost -u " db-user " -p" db-pass 
        " " db-name " > /home/dataBackupSource/transport-outgoing/"
        (common/backup-file-name app instance-name :mysql))
   (str "chown dataBackupSource:dataBackupSource /home/dataBackupSource/transport-outgoing/"
        (common/backup-file-name app instance-name :mysql))
   ""]
  )

(defn- remove-old-gens
  ""
  [app instance-name  gens-stored-on-source-system type]
  (let [file-name-pattern 
        (str (common/backup-file-prefix app instance-name type) "_*")]
    (str "  (ls -t " file-name-pattern "|head -n " gens-stored-on-source-system
          ";ls " file-name-pattern")|sort|uniq -u|xargs rm -r")
    )
  )

(defn source-transport-script-lines
  ""
  [& {:keys [app-name 
             instance-name 
             gens-stored-on-source-system 
             files-to-transport]
      }]
   {:pre [(not (nil? gens-stored-on-source-system))
         (not (nil? app-name))
         (not (nil? instance-name))
         (not (nil? files-to-transport))
         (vector? files-to-transport)]}
  (into []
        (concat 
          ["# Move transported files to store"
           "mv /home/dataBackupSource/transport-outgoing/* /home/dataBackupSource/store"
           ""
           "# Manage old backup generations"
           "cd /home/dataBackupSource/store"
           "# test wether pwd points to expected place"
           "if [ \"$PWD\" == \"/home/dataBackupSource/store\" ]; then"]
          (into []
           (map #(remove-old-gens app-name instance-name 
                   gens-stored-on-source-system %) files-to-transport))
          ["fi"
          ""]
          ))
    )