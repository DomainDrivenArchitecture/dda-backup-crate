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


(ns org.domaindrivenarchitecture.pallet.crate.backup.restore-lib
  (require 
    [org.domaindrivenarchitecture.pallet.crate.backup.common-lib :as common]))

(def restore-parameters
  ["if [ -z \"$1\" ]; then"
   "  echo \"\""
   "  echo \"usage:\""
   "  echo \"restore.sh [file_name_prefix]\""
   "  echo \"  file_name_prefix: mandantory, the file name prefix for the files to restore like 'liferay_pa-prod'.\""
   "  echo \"\""
   "  echo \"Example 'restore.sh liferay_pa-prod' will use the newest backup-files with the pattern iferay_pa-prod_mysql_* and iferay_pa-prod_file_*\""                                                                                                                                              
   "  exit 1"                                                                                                                                       
   "fi"
   ""]
  )

(def restore-navigate-to-restore-location
  ["# cd to restore location"
   "cd /home/dataBackupSource/restore"
   ""])

(defn restore-locate-restore-dumps
  ""
  []
  ["# Get the dumps"
   (str "most_recent_sql_dump=$(ls -d -t1 $1" 
        (common/file-type-name :mysql)
        "_* | head -n1)")
   (str "most_recent_file_dump=$(ls -d -t1 $1" 
        (common/file-type-name :file-compressed) 
        "_* | head -n1)")
   ""
   "echo \"using this inputs:\""
   "echo \"$most_recent_sql_dump\""
   "echo \"$most_recent_file_dump\""
   ""]
  )

(def restore-head 
  ["if [ \"$most_recent_sql_dump\" ] && [ \"$most_recent_file_dump\" ]; then"
   "  echo \"starting restore\""
   "  "
   ])

(def restore-tail 
  ["  echo \"finished restore successfull, pls. start the appserver.\""
   "fi"
   ""])

(def restore-db-head
  ["  # ------------- restore db --------------"
   "  echo \"db restore ...\""
   "  "])

(def restore-db-tail
  ["  echo \"finished db restore\""
   "  "])

(defn restore-mysql
  ""
  [& {:keys [db-user 
             db-pass 
             db-name 
             dump-filename 
             create-options]
      :or {dump-filename "${most_recent_sql_dump}"
           create-options nil}}]
  (let [used-create-options (if create-options
                              (str " " create-options)
                              "")]
    [(str "mysql -hlocalhost -u" db-user " -p" db-pass " -e \"drop database " db-name "\";")
     (str "mysql -hlocalhost -u" db-user " -p" db-pass " -e \"create database " 
          db-name used-create-options "\";")
     (str "mysql -hlocalhost -u" db-user " -p" db-pass " " db-name " < " dump-filename)
     ""
     ])
  )

(def restore-file-head
  ["  # ------------- restore file --------------"
   "  echo \"file restore ...\""
   "  "])

(def restore-file-tail
  ["  echo \"finished file restore.\""
   "  "])

(defn restore-tar
  ""
  [& {:keys [restore-target-dir
             file-type
             new-owner]
      :or {dump-filename "${most_recent_sql_dump}"
           file-type :file-compressed}}]
  (let [chown
        (if new-owner
          [(str "chown -R " new-owner ":" new-owner " " restore-target-dir)]
          [])
        tar-onwe-options 
        (if new-owner
          ""
          "--same-owner --same-permissions ")
        tar-compress-option 
        (case file-type
          :file-compressed "z"
          :file-plain "")
        ]
    (into []
          (concat 
            [(str "rm -r " restore-target-dir "/*")
             (str "tar " 
                  tar-onwe-options 
                  "-x" 
                  tar-compress-option 
                  "f ${most_recent_file_dump} -C "
                  restore-target-dir)]
            chown
            [""]
            ))
    )
  )

(defn restore-rsync
  [& {:keys [restore-target-dir
             new-owner]
      :or {dump-filename "${most_recent_sql_dump}"}
      }]
  (into []
        (concat 
          [(str "rm -r " restore-target-dir "/*")
           (str "rsync -Aax"  
                " ${most_recent_file_dump}/"
                restore-target-dir
                "/ "
                restore-target-dir)]
          (if new-owner
            [(str "chown -R " new-owner ":" new-owner " " restore-target-dir)]
            [])
          [""]
          ))
  )
