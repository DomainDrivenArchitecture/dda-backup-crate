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

(def liferay-elements
   [{:type :file-compressed
     :name "letsencrypt"
     :backup-file-name "service-name_letsencrypt_file_${timestamp}.tgz"
     :backup-file-prefix-pattern "service-name_letsencrypt_file_*"
     :type-name "file"
     :root-dir "/etc/letsencrypt/"
     :subdir-to-save "accounts csr keys renewal"}
    {:type :file-compressed
     :name "liferay"
     :backup-file-name "service-name_liferay_file_${timestamp}.tgz"
     :backup-file-prefix-pattern "service-name_liferay_file_*"
     :type-name "file"
     :root-dir "/var/lib/liferay/data/"
     :subdir-to-save "document_library images"
     :new-owner "tomcat7"}
    {:type :mysql
     :name "liferay"
     :backup-file-name "service-name_liferay_mysql_${timestamp}.sql"
     :backup-file-prefix-pattern "service-name_liferay_mysql_*"
     :type-name "mysql"
     :db-user-name "db-user-name"
     :db-user-passwd "db-pass"
     :db-name "db-name"
     :db-pre-processing (pre-process "fqdn")
     :db-post-processing (post-process "fqdn" "db-user-name" "db-pass" "db-name")
     :db-create-options "character set utf8"}])

(def letsencrypt-only-element
   [{:type :file-compressed
     :backup-file-name "service-name_letsencrypt_file_${timestamp}.tgz"
     :backup-file-prefix-pattern "service-name_letsencrypt_file_*"
     :type-name "file"
     :name "letsencrypt"
     :root-dir "/etc/letsencrypt/"
     :subdir-to-save "accounts csr keys renewal"}])

(def transport-ssh
  {:ssh-pull true})


(deftest backup-script-with-service
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
            "tar cvzf /var/backups/transport-outgoing/service-name_letsencrypt_file_${timestamp}.tgz accounts csr keys renewal"
            "chown dda-backup:dda-backup /var/backups/transport-outgoing/service-name_letsencrypt_file_${timestamp}.tgz"
            ""
            "#backup the files"
            "cd /var/lib/liferay/data/"
            "tar cvzf /var/backups/transport-outgoing/service-name_liferay_file_${timestamp}.tgz document_library images"
            "chown dda-backup:dda-backup /var/backups/transport-outgoing/service-name_liferay_file_${timestamp}.tgz"
            ""
            "#backup db"
            "mysqldump --no-create-db=true -h localhost -u db-user-name -pdb-pass db-name > /var/backups/transport-outgoing/service-name_liferay_mysql_${timestamp}.sql"
            "chown dda-backup:dda-backup /var/backups/transport-outgoing/service-name_liferay_mysql_${timestamp}.sql"
            ""
            "#start appserver"
            "service tomcat7 start"
            ""]
          (sut/backup-script-lines
           "service-name"
           "/var/backups/transport-outgoing"
           "tomcat7"
           "dda-backup"
           liferay-elements)))))

(deftest backup-script-without-service
  (testing
    "script content for service nil"
    (is (= ["#!/bin/bash"
            ""
            "#timestamp from server to variable"
            "export timestamp=`date +%Y-%m-%d_%H-%M-%S`"
            ""
            "#backup the files"
            "cd /etc/letsencrypt/"
            "tar cvzf /var/backups/transport-outgoing/service-name_letsencrypt_file_${timestamp}.tgz accounts csr keys renewal"
            "chown dda-backup:dda-backup /var/backups/transport-outgoing/service-name_letsencrypt_file_${timestamp}.tgz"
            ""]
           (sut/backup-script-lines
            "service-name"
            "/var/backups/transport-outgoing"
            nil
            "dda-backup"
            letsencrypt-only-element))))
  (testing
    "script content for service empty"
    (is (= (sut/backup-script-lines
             "service-name"
             "/var/backups/transport-outgoing"
             nil
             "dda-backup"
             letsencrypt-only-element)
           (sut/backup-script-lines
            "service-name"
            "/var/backups/transport-outgoing"
            ""
            "dda-backup"
            letsencrypt-only-element)))))


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
           (sut/transport-script-lines liferay-elements)))))


(deftest restore-script
  (testing
    "script content"
    (is (= ["#!/bin/bash"
            ""
            "# cd to restore location"
            "cd /var/backups/restore"
            ""
            "# Get the dumps"
            "most_recent_letsencrypt_file_dump=$(ls -d -t1 service-name_letsencrypt_file_* | head -n1)"
            "most_recent_liferay_file_dump=$(ls -d -t1 service-name_liferay_file_* | head -n1)"
            "most_recent_liferay_mysql_dump=$(ls -d -t1 service-name_liferay_mysql_* | head -n1)"
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
           (sut/restore-script-lines
            false
            "/usr/lib/local/dda-backup"
            "/var/backups/restore"
            "tomcat7"
            transport-ssh
            liferay-elements)))))


(deftest restore-script-without-service
  (testing
    "script content"
    (is (= ["#!/bin/bash"
            ""
            "# cd to restore location"
            "cd /var/backups/restore"
            ""
            "# Get the dumps"
            "most_recent_letsencrypt_file_dump=$(ls -d -t1 service-name_letsencrypt_file_* | head -n1)"
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
           (sut/restore-script-lines
            false
            "/usr/lib/local/dda-backup"
            "/var/backups/restore"
            ""
            transport-ssh
            letsencrypt-only-element)))))
