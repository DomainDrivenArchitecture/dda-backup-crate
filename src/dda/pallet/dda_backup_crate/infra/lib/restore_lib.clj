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


(ns dda.pallet.dda-backup-crate.infra.lib.restore-lib
  (require
   [schema.core :as s]
   [dda.pallet.dda-backup-crate.infra.schema :as schema]))

(def restore-parameters
  ["if [ -z \"$1\" ]; then"
   "  echo \"\""
   "  echo \"usage:\""
   "  echo \"restore.sh [file_name_prefix]\""
   "  echo \"  file_name_prefix: mandantory, the file name prefix for the files to restore like 'liferay_pa-prod'.\""
   "  echo \"\""
   "  echo \"Example 'restore.sh pa-prod' will use the newest backup-files with the pattern iferay_pa-prod_mysql_* and iferay_pa-prod_file_*\""
   "  exit 1"
   "fi"
   ""])

(s/defn restore-navigate-to-restore-location
  [backup-restore-folder :- s/Str]
  ["# cd to restore location"
   (str "cd " backup-restore-folder)
   ""])

(s/defn restore-dump-name
  "Get the newest file for restore."
  [backup-element :- schema/BackupElement]
  (let [{:keys [name type-name]} backup-element]
    (str "most_recent_" name "_" type-name "_dump")))

(s/defn get-restore-dump
  "Get the newest file for restore."
  [backup-element :- schema/BackupElement]
  (let [{:keys [backup-file-prefix-pattern]} backup-element]
    [(str (restore-dump-name backup-element)
          "=$(ls -d -t1 $1"
          backup-file-prefix-pattern
          "_* | head -n1)")]))

(s/defn echo-restore-dump
  "Echo used file for restore."
  [element :- schema/BackupElement]
  [(str "echo \"$"
        (restore-dump-name element)
        "\"")])

(defn provide-restore-dumps
  "Provide the most recent files for restore."
  [elements]
  (into
   []
   (concat
    ["# Get the dumps"]
    (mapcat get-restore-dump elements)
    [""
     "echo \"using this inputs:\""]
    (mapcat echo-restore-dump elements)
    [""])))

(s/defn restore-head-element
  [element :- schema/BackupElement]
  (str "[ \"$" (restore-dump-name element) "\" ]"))

(defn restore-head-script
  [elements]
  [(str "if "
        (clojure.string/join
         " && "
         (map restore-head-element elements))
        "; then")
   "echo \"starting restore\""
   ""])

(def restore-tail
  ["echo \"finished restore successfull, pls. start the appserver.\""
   "fi"
   ""])

(def restore-db-head
  ["# ------------- restore db --------------"
   "echo \"db restore ...\""
   ""])

(def restore-db-tail
  ["echo \"finished db restore\""
   ""])

(s/defn restore-mysql-dump :- [s/Str]
  "lines for restoring a mysql dump"
  [element :- schema/BackupElement]
  (let [{:keys [db-user-name db-user-passwd db-name db-create-options]
         :or {db-create-options ""}} element]
    [(str "mysql -hlocalhost -u" db-user-name " -p" db-user-passwd " -e \"drop database " db-name "\";")
     (str "mysql -hlocalhost -u" db-user-name " -p" db-user-passwd " -e \"create database "
          db-name db-create-options "\";")
     (str "mysql -hlocalhost -u" db-user-name " -p" db-user-passwd " " db-name " < ${" (restore-dump-name element) "}")
     ""]))

(s/defn restore-mysql-script :- [s/Str]
  "The script for restoring mysql."
  [element :- schema/BackupElement]
  (let [{:keys [db-pre-processing db-post-processing]} element]
    (into
     []
     (concat
      restore-db-head
      (when (contains? element :db-pre-processing)
        db-pre-processing)
      (restore-mysql-dump element)
      (when (contains? element :db-post-processing)
        db-post-processing)
      restore-db-tail))))

(def restore-file-head
  ["# ------------- restore file --------------"
   "echo \"file restore ...\""
   ""])

(def restore-file-tail
  ["echo \"finished file restore.\""
   ""])

(s/defn restore-tar-dump :- [s/Str]
  "restore files from a tar dump."
  [element :- schema/BackupElement]
  (let [{:keys [root-dir new-owner type]} element
        chown (if (contains? element :new-owner)
                [(str "chown -R " new-owner
                      ":" new-owner
                      " " root-dir)]
                [])
        tar-own-options (if (contains? element :new-owner)
                          ""
                          "--same-owner --same-permissions ")
        tar-compress-option (if (= type :file-compressed)
                              "z"
                              "")]
    (into
     []
     (concat
        ;TODO: Use NonRootDirectory type here!
      [(str "rm -r " root-dir "/*")
       (str "tar " tar-own-options "-x" tar-compress-option "f ${" (restore-dump-name element) "} -C " root-dir)]
      chown
      [""]))))

(s/defn restore-tar-script :- [s/Str]
  "The script for restoring a tar file."
  [element :- schema/BackupElement]
  (into
   []
   (concat
    restore-file-head
    (restore-tar-dump element)
    restore-file-tail)))

(defn restore-rsync
  [& {:keys [restore-target-dir
             new-owner]
      :or {dump-filename "${most_recent_sql_dump}"}}]
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
         [""])))
