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

(ns dda.pallet.dda-backup-crate.infra.backup-elements-test
  (:require
    [schema.core :as s]
    [clojure.test :refer :all]
    [dda.pallet.commons.plan-test-utils :as tu]
    [dda.pallet.dda-backup-crate.infra.backup-elements :as sut]))

(defn pre-process
  [fqdn]
  ["# replace location in portal config"
   (str
     "sedHttps=\"s/<name>cdn.host.https<\\/name>"
     "<value>https:\\/\\/" fqdn "<\\/value>/"
     "<name>cdn.host.https<\\/name><value>https:\\/\\/"
     fqdn "<\\/value>/\"")
   (str "sedHttp=\"s/<name>cdn.host.http<\\/name>"
        "<value>http:\\/\\/" fqdn "<\\/value>/"
        "<name>cdn.host.http<\\/name><value>http:\\/\\/"
        fqdn "<\\/value>/\"")
   "sed -e \"$sedHttps\" ${most_recent_liferay_mysql_dump} > output1.sql"
   "sed -e \"$sedHttp\" output1.sql > output2.sql"
   "move output2.sql ${most_recent_liferay_mysql_dump}"
   ""])


(defn post-process
  [fqdn db-user-name db-pass db-name]
  ["#db-restore postprocessing"
   (str "mysql -hlocalhost -u" db-user-name " -p" db-pass
        " -D" db-name
        " -e \"update Company set webId = '"
        fqdn "', mx = '"
        fqdn "' where companyId = 10132;\"")
   (str "mysql -hlocalhost -u" db-user-name " -p" db-pass
        " -D" db-name
        " -e \"update VirtualHost set hostname = '"
        fqdn "' where virtualHostId = 35337;\"")
   ""])


(def backup-user {:name "dataBackupSource"
                  :encrypted-passwd "WIwn6jIUt2Rbc"})

(def liferay-config-elements
   [{:type :file-compressed
     :name "letsencrypt"
     :root-dir "/etc/letsencrypt/"
     :subdir-to-save "accounts csr keys renewal"
     {:type :file-compressed
      :name "liferay"
      :root-dir "/var/lib/liferay/data/"
      :subdir-to-save "document_library images"
      :new-owner "tomcat7"}
     {:type :mysql
      :name "liferay"
      :db-user-name "db-user-name"
      :db-user-passwd "db-pass"
      :db-name "db-name"
      :db-pre-processing (pre-process "fqdn")
      :db-post-processing (post-process "fqdn" "db-user-name" "db-pass" "db-name")
      :db-create-options "character set utf8"}}])

(def liferay-config
   {:backup-name "service-name"
    :backup-user backup-user
    :service-restart "tomcat7"
    :elements [{:type :file-compressed
                :name "letsencrypt"
                :root-dir "/etc/letsencrypt/"
                :subdir-to-save "accounts csr keys renewal"}
               {:type :file-compressed
                :name "liferay"
                :root-dir "/var/lib/liferay/data/"
                :subdir-to-save "document_library images"
                :new-owner "tomcat7"}
               {:type :mysql
                :name "liferay"
                :db-user-name "db-user-name"
                :db-user-passwd "db-pass"
                :db-name "db-name"
                :db-pre-processing (pre-process "fqdn")
                :db-post-processing (post-process "fqdn" "db-user-name" "db-pass" "db-name")
                :db-create-options "character set utf8"}]})

(def service-less-config
   {:backup-name "backup-name"
    :backup-user backup-user
    :elements [{:type :file-compressed
                :name "letsencrypt"
                :root-dir "/etc/letsencrypt/"
                :subdir-to-save "accounts csr keys renewal"}]})

(deftest backup-script
  (testing
    "script content"
    (is (= ["#!/bin/bash"
            ""
            "#timestamp from server to variable"
            "export timestamp=`date +%Y-%m-%d_%H-%M-%S`"
            ""
            "#stop appserver"
            "service tomcat7 stop"
            ""
            "#backup the files"
            "cd /etc/letsencrypt/"
            "tar cvzf /home/dataBackupSource/transport-outgoing/service-name_letsencrypt_file_${timestamp}.tgz accounts csr keys renewal"
            "chown dataBackupSource:dataBackupSource /home/dataBackupSource/transport-outgoing/service-name_letsencrypt_file_${timestamp}.tgz"
            ""
            "#backup the files"
            "cd /var/lib/liferay/data/"
            "tar cvzf /home/dataBackupSource/transport-outgoing/service-name_liferay_file_${timestamp}.tgz document_library images"
            "chown dataBackupSource:dataBackupSource /home/dataBackupSource/transport-outgoing/service-name_liferay_file_${timestamp}.tgz"
            ""
            "#backup db"
            "mysqldump --no-create-db=true -h localhost -u db-user-name -pdb-pass db-name > /home/dataBackupSource/transport-outgoing/service-name_liferay_mysql_${timestamp}.sql"
            "chown dataBackupSource:dataBackupSource /home/dataBackupSource/transport-outgoing/service-name_liferay_mysql_${timestamp}.sql"
            ""
            "#start appserver"
            "service tomcat7 start"
            ""]
          (sut/backup-script-lines
           "service-name"
           ""
           "tomcat7"
           ""
           liferay-config)))))

(deftest backup-script-without-service
  (testing
    "script content"
    (is (= ["#!/bin/bash"
            ""
            "#timestamp from server to variable"
            "export timestamp=`date +%Y-%m-%d_%H-%M-%S`"
            ""
            "#backup the files"
            "cd /etc/letsencrypt/"
            "tar cvzf /home/dataBackupSource/transport-outgoing/backup-name_letsencrypt_file_${timestamp}.tgz accounts csr keys renewal"
            "chown dataBackupSource:dataBackupSource /home/dataBackupSource/transport-outgoing/backup-name_letsencrypt_file_${timestamp}.tgz"
            ""]
           (sut/backup-script-lines service-less-config)))))


(deftest test-transport-script-lines
  (testing
    "script content"
    (is (= ["#!/bin/bash"
             ""
             "# Move transported files to store"
             "mv /home/dataBackupSource/transport-outgoing/* /home/dataBackupSource/store"
             ""
             "# Manage old backup generations"
             "cd /home/dataBackupSource/store"
             "# test wether pwd points to expected place"
             "if [ \"$PWD\" == \"/home/dataBackupSource/store\" ]; then"
             "  (ls -t service-name_letsencrypt_file_*|head -n 3;ls service-name_letsencrypt_file_*)|sort|uniq -u|xargs rm -r"
             "  (ls -t service-name_liferay_file_*|head -n 3;ls service-name_liferay_file_*)|sort|uniq -u|xargs rm -r"
             "  (ls -t service-name_liferay_mysql_*|head -n 3;ls service-name_liferay_mysql_*)|sort|uniq -u|xargs rm -r"
             "fi"
             ""]
           (sut/transport-script-lines liferay-config)))))


(deftest restore-script
  (testing
    "script content"
    (is (= ["#!/bin/bash"
            ""
            "if [ -z \"$1\" ]; then"
            "  echo \"\""
            "  echo \"usage:\""
            "  echo \"restore.sh [file_name_prefix]\""
            "  echo \"  file_name_prefix: mandantory, the file name prefix for the files to restore like 'liferay_pa-prod'.\""
            "  echo \"\""
            "  echo \"Example 'restore.sh pa-prod' will use the newest backup-files with the pattern iferay_pa-prod_mysql_* and iferay_pa-prod_file_*\""
            "  exit 1"
            "fi"
            ""
            "# cd to restore location"
            "cd /home/dataBackupSource/restore"
            ""
            "# Get the dumps"
            "most_recent_letsencrypt_file_dump=$(ls -d -t1 $1letsencrypt_file_* | head -n1)"
            "most_recent_liferay_file_dump=$(ls -d -t1 $1liferay_file_* | head -n1)"
            "most_recent_liferay_mysql_dump=$(ls -d -t1 $1liferay_mysql_* | head -n1)"
            ""
            "echo \"using this inputs:\""
            "echo \"$most_recent_letsencrypt_file_dump\""
            "echo \"$most_recent_liferay_file_dump\""
            "echo \"$most_recent_liferay_mysql_dump\""
            ""
            "if [ \"$most_recent_letsencrypt_file_dump\" ] && [ \"$most_recent_liferay_file_dump\" ] && [ \"$most_recent_liferay_mysql_dump\" ]; then"
            "echo \"starting restore\""
            ""
            "#stop appserver"
            "service tomcat7 stop"
            ""
            "# ------------- restore file --------------"
            "echo \"file restore ...\""
            ""
            "rm -r /etc/letsencrypt//*"
            "tar --same-owner --same-permissions -xzf ${most_recent_letsencrypt_file_dump} -C /etc/letsencrypt/"
            ""
            "echo \"finished file restore.\""
            ""
            "# ------------- restore file --------------"
            "echo \"file restore ...\""
            ""
            "rm -r /var/lib/liferay/data//*"
            "tar -xzf ${most_recent_liferay_file_dump} -C /var/lib/liferay/data/"
            "chown -R tomcat7:tomcat7 /var/lib/liferay/data/"
            ""
            "echo \"finished file restore.\""
            ""
            "# ------------- restore db --------------"
            "echo \"db restore ...\""
            ""
            "# replace location in portal config"
            "sedHttps=\"s/<name>cdn.host.https<\\/name><value>https:\\/\\/fqdn<\\/value>/<name>cdn.host.https<\\/name><value>https:\\/\\/fqdn<\\/value>/\""
            "sedHttp=\"s/<name>cdn.host.http<\\/name><value>http:\\/\\/fqdn<\\/value>/<name>cdn.host.http<\\/name><value>http:\\/\\/fqdn<\\/value>/\""
            "sed -e \"$sedHttps\" ${most_recent_liferay_mysql_dump} > output1.sql"
            "sed -e \"$sedHttp\" output1.sql > output2.sql"
             "move output2.sql ${most_recent_liferay_mysql_dump}"
             ""
             "mysql -hlocalhost -udb-user-name -pdb-pass -e \"drop database db-name\";"
             "mysql -hlocalhost -udb-user-name -pdb-pass -e \"create database db-namecharacter set utf8\";"
             "mysql -hlocalhost -udb-user-name -pdb-pass db-name < ${most_recent_liferay_mysql_dump}"
             ""
             "#db-restore postprocessing"
             "mysql -hlocalhost -udb-user-name -pdb-pass -Ddb-name -e \"update Company set webId = 'fqdn', mx = 'fqdn' where companyId = 10132;\""
             "mysql -hlocalhost -udb-user-name -pdb-pass -Ddb-name -e \"update VirtualHost set hostname = 'fqdn' where virtualHostId = 35337;\""
             ""
             "echo \"finished db restore\""
             ""
             "echo \"finished restore successfull, pls. start the appserver.\""
             "fi"
             ""]
           (sut/restore-script-lines liferay-config)))))


(deftest restore-script-without-service
  (testing
    "script content"
    (is (= ["#!/bin/bash"
            ""
            "if [ -z \"$1\" ]; then"
            "  echo \"\""
            "  echo \"usage:\""
            "  echo \"restore.sh [file_name_prefix]\""
            "  echo \"  file_name_prefix: mandantory, the file name prefix for the files to restore like 'liferay_pa-prod'.\""
            "  echo \"\""
            "  echo \"Example 'restore.sh pa-prod' will use the newest backup-files with the pattern iferay_pa-prod_mysql_* and iferay_pa-prod_file_*\""
            "  exit 1"
            "fi"
            ""
            "# cd to restore location"
            "cd /home/dataBackupSource/restore"
            ""
            "# Get the dumps"
            "most_recent_letsencrypt_file_dump=$(ls -d -t1 $1letsencrypt_file_* | head -n1)"
            ""
            "echo \"using this inputs:\""
            "echo \"$most_recent_letsencrypt_file_dump\""
            ""
            "if [ \"$most_recent_letsencrypt_file_dump\" ]; then"
            "echo \"starting restore\""
            ""
            "# ------------- restore file --------------"
            "echo \"file restore ...\""
            ""
            "rm -r /etc/letsencrypt//*"
            "tar --same-owner --same-permissions -xzf ${most_recent_letsencrypt_file_dump} -C /etc/letsencrypt/"
            ""
            "echo \"finished file restore.\""
            ""
             "echo \"finished restore successfull, pls. start the appserver.\""
             "fi"
             ""]
           (sut/restore-script-lines service-less-config)))))



; (deftest write-scripts
;   (testing
;     "test write-script actions"
;     (is (.contains
;           (tu/extract-nth-action-command
;             (build-actions/build-actions
;               build-actions/ubuntu-session
;               (sut/write-scripts liferay-config))
;               1)
;           "service-name_backup.sh"))
;     ))
