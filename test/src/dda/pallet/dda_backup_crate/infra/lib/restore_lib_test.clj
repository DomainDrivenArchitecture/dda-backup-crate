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

(ns dda.pallet.dda-backup-crate.infra.lib.restore-lib-test
  (:require
   [clojure.test :refer :all]
   [pallet.actions :as actions]
   ;[dda.pallet.dda-backup-crate.infra.core.backup-element-test :as backup-element]
   [dda.pallet.dda-backup-crate.infra.lib.restore-lib :as sut]))


(deftest restore-mysql
  (testing
   "restore for owncloud"
    (is (= ["# ------------- restore db --------------"
            "echo \"db restore ...\""
            ""
            "mysql -hlocalhost -uowncloud -powncloud-db-pwd -e \"drop database owncloud\";"
            "mysql -hlocalhost -uowncloud -powncloud-db-pwd -e \"create database owncloud\";"
            "mysql -hlocalhost -uowncloud -powncloud-db-pwd owncloud < ${most_recent_name_mysql_dump}"
            ""
            "echo \"finished db restore\""]
           (clojure.string/split-lines
             (sut/restore-element
                {:type :mysql
                 :name "name"
                 :type-name "mysql"
                 :db-user-name "owncloud"
                 :db-user-passwd "owncloud-db-pwd"
                 :db-name "owncloud"}))))))

(deftest restore-tar
  (testing
   "restore for owncloud"
    (is (= ["# ------------- restore file --------------"
            "echo \"file restore ...\""
            ""
            "rm -r /var/www/owncloud/*"
            "tar --same-owner --same-permissions -xf ${most_recent_name_file_dump} -C /"
            ""
            "echo \"finished file restore.\""]
           (clojure.string/split-lines
             (sut/restore-element
               {:type :file-plain
                :name "name"
                :type-name "file"
                :backup-path ["/var/www/owncloud/*"]})))))
  (testing
   "restore for liferay"
    (is (= ["# ------------- restore file --------------"
            "echo \"file restore ...\""
            ""
            "rm -r /var/lib/liferay/data/*"
            "tar -xzf ${most_recent_name_file_dump} -C /"
            ""
            "chown -R tomcat7:tomcat7 /var/lib/liferay/data/*"
            ""
            "echo \"finished file restore.\""]
           (clojure.string/split-lines
             (sut/restore-element
               {:type :file-compressed
                :name "name"
                :type-name "file"
                :backup-path ["/var/lib/liferay/data/*"]
                :new-owner "tomcat7"}))))))

; (deftest restore-duplicity
;   (testing
;    "restore with duplicity"
;     (is (=
;          ["# Transport Backup"
;           "export PASSPHRASE= "
;           "export TMPDIR=/var/opt/gitblit/backup-cache"
;           "export AWS_ACCESS_KEY_ID=A1"
;           "export AWS_SECRET_ACCESS_KEY=A1"
;           "export S3_USE_SIGV4=True"
;           backup-element/prep-restore-script
;           "/usr/bin/duplicity restore --gpg-binary gpg2 --archive-dir /var/opt/gitblit/backup-cache --verbosity notice --s3-use-new-style --s3-european-buckets --encrypt-key=1A --sign-key=1A --log-file /var/log/gitblit/duplicity.log localhost /var/opt/gitblit/backups"
;           backup-element/post-transport-script
;           "unset AWS_ACCESS_KEY_ID"
;           "unset AWS_SECRET_ACCESS_KEY"
;           "unset S3_USE_SIGV4"
;           "unset PASSPHRASE"
;           "unset TMPDIR"
;           ""]
;          (sut/restore-duplicity backup-element/test-element)))))
