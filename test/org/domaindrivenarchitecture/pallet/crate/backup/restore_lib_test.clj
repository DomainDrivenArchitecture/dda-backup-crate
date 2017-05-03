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

(ns org.domaindrivenarchitecture.pallet.crate.backup.restore-lib-test
  (:require
   [clojure.test :refer :all]
   [pallet.actions :as actions]
   [org.domaindrivenarchitecture.pallet.crate.backup.restore-lib :as sut]))

(deftest restore-mysql
  (testing
   "restore for owncloud"
    (is (= ["# ------------- restore db --------------"
            "echo \"db restore ...\""
            ""
            "mysql -hlocalhost -uowncloud -powncloud-db-pwd -e \"drop database owncloud\";"
            "mysql -hlocalhost -uowncloud -powncloud-db-pwd -e \"create database owncloud\";";
            "mysql -hlocalhost -uowncloud -powncloud-db-pwd owncloud < ${most_recent_name_mysql_dump}"
            ""
            "echo \"finished db restore\""
            ""]
           (sut/restore-mysql-script
            {:type :mysql
             :name "name"
             :db-user-name "owncloud"
             :db-user-passwd "owncloud-db-pwd"
             :db-name "owncloud"})))))

(deftest restore-tar
  (testing
   "restore for owncloud"
    (is (= ["# ------------- restore file --------------"
            "echo \"file restore ...\""
            ""
            "rm -r /var/www/owncloud/*"
            "tar --same-owner --same-permissions -xf ${most_recent_name_file_dump} -C /var/www/owncloud"
            ""
            "echo \"finished file restore.\""
            ""]
           (sut/restore-tar-script
            {:type :file-plain
             :name "name"
             :subdir-to-save "./"
             :root-dir "/var/www/owncloud"}))))
  (testing
   "restore for liferay"
    (is (= ["# ------------- restore file --------------"
            "echo \"file restore ...\""
            ""
            "rm -r /var/lib/liferay/data/*"
            "tar -xzf ${most_recent_name_file_dump} -C /var/lib/liferay/data"
            "chown -R tomcat7:tomcat7 /var/lib/liferay/data"
            ""
            "echo \"finished file restore.\""
            ""]
           (sut/restore-tar-script
            {:type :file-compressed
             :name "name"
             :root-dir "/var/lib/liferay/data"
             :subdir-to-save "./"
             :new-owner "tomcat7"})))))

(def options-dup [:archive-dir "/var/opt/gitlab/backup-cache"
                  :verbosity :notice
                  :s3-use-new-style true
                  :s3-european-buckets true
                  :encrypt-key=1A true
                  :sign-key=1A true
                  :log-file "/var/log/gitlab/duplicity.log"])

(def prep-script "[[ -d /var/opt/gitlab/backup-cache ]] || mkdir /var/opt/gitlab/backup-cache")
(def post-script "/bin/tar -xzf /var/opt/gitlab/backups/gitlab_secrets.tgz --directory=/ && /usr/bin/gitlab-ctl stop unicorn && /usr/bin/gitlab-ctl stop sidekiq && /usr/bin/gitlab-rake gitlab:backup:restore && /usr/bin/sudo gitlab-ctl start && /usr/bin/sudo gitlab-rake gitlab:check SANITIZE=true")

(def dup-backup-element {:type :duplicity
                         :tmp-dir "/var/opt/gitlab/backup-cache"
                         :passphrase " "
                         :aws-access-key-id "A1"
                         :aws-secret-access-key "A1"
                         :s3-use-sigv4 "True"
                         :action :full
                         :options {:backup-options [:dummy :options]
                                   :restore-options options-dup}
                         :directory "/var/opt/gitlab/backups"
                         :url "localhost"
                         :prep-scripts {:prep-backup-script "dummy-script"
                                        :prep-restore-script prep-script}
                         :post-ops {:remove-remote-backup {:days 0
                                                           :options [:dummy :options]}
                                    :post-transport-script post-script}})

(deftest restore-duplicity
  (testing
   "restore with duplicity"
    (is (=
         ["# Transport Backup"
          "export PASSPHRASE= "
          "export TMPDIR=/var/opt/gitlab/backup-cache"
          "export AWS_ACCESS_KEY_ID=A1"
          "export AWS_SECRET_ACCESS_KEY=A1"
          "export S3_USE_SIGV4=True"
          prep-script
          "/usr/bin/duplicity restore --archive-dir /var/opt/gitlab/backup-cache --verbosity notice --s3-use-new-style --s3-european-buckets --encrypt-key=1A --sign-key=1A --log-file /var/log/gitlab/duplicity.log localhost /var/opt/gitlab/backups"
          post-script
          "unset AWS_ACCESS_KEY_ID"
          "unset AWS_SECRET_ACCESS_KEY"
          "unset S3_USE_SIGV4"
          "unset PASSPHRASE"
          "unset TMPDIR"
          ""]
         (sut/restore-duplicity dup-backup-element)))))
