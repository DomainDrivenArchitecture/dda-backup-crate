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

(ns org.domaindrivenarchitecture.pallet.crate.backup.backup-lib-test
  (:require
   [clojure.test :refer :all]
   [schema.core :as s]
   [org.domaindrivenarchitecture.pallet.crate.backup.backup-lib :as sut]))

(deftest transport-lines
  (testing
   "backup mysql"
    (is (= ["#backup db"
            "mysqldump --no-create-db=true -h localhost -u prod -ppwd lportal > /home/dataBackupSource/transport-outgoing/portal_prod_mysql_${timestamp}.sql"
            "chown dataBackupSource:dataBackupSource /home/dataBackupSource/transport-outgoing/portal_prod_mysql_${timestamp}.sql"
            ""]
           (sut/backup-mysql
            "portal"
            {:type :mysql
             :name "prod"
             :db-user-name "prod"
             :db-user-passwd "pwd"
             :db-name "lportal"})))))

(deftest backup-files-tar
  (testing
   "backup files as compressed archive"
    (is (= ["#backup the files"
            "cd /var/lib/liferay/data/"
            "tar cvzf /home/dataBackupSource/transport-outgoing/portal_prod_file_${timestamp}.tgz document_library"
            "chown dataBackupSource:dataBackupSource /home/dataBackupSource/transport-outgoing/portal_prod_file_${timestamp}.tgz"
            ""]
           (sut/backup-files-tar
            "portal"
            {:type :file-compressed
             :name "prod"
             :root-dir "/var/lib/liferay/data/"
             :subdir-to-save "document_library"}))))
  (testing
   "backup files as uncompressed archive"
    (is (= ["#backup the files"
            "cd /var/lib/liferay/data/"
            "tar cvf /home/dataBackupSource/transport-outgoing/portal_prod_file_${timestamp}.tar document_library"
            "chown dataBackupSource:dataBackupSource /home/dataBackupSource/transport-outgoing/portal_prod_file_${timestamp}.tar"
            ""]
           (sut/backup-files-tar
            "portal"
            {:type :file-plain
             :name "prod"
             :root-dir "/var/lib/liferay/data/"
             :subdir-to-save "document_library"})))))

(def options-dup [:archive-dir "/var/opt/gitlab/backup-cache"
                  :verbosity :notice
                  :s3-use-new-style true
                  :s3-european-buckets true
                  :encrypt-key=1A true
                  :sign-key=1A true])

(def add-dup-op-backup [:asynchronous-upload true
                        :volsize=1500 true
                        :log-file "/var/log/gitlab/duplicity.log"])

(def add-dup-op-delete-old [:log-file "/var/log/gitlab/duplicity.log"
                            :force true])

(def dup-backup-element {:type :duplicity
                         :tmp-dir "/var/opt/gitlab/backup-cache"
                         :trust-script-path " "
                         :priv-key-path " "
                         :pub-key-path " "
                         :passphrase " "
                         :aws-access-key-id "A1"
                         :aws-secret-access-key "A1"
                         :s3-use-sigv4 "True"
                         :action :full
                         :options {:backup-options (into [] (concat options-dup add-dup-op-backup))
                                   :restore-options [:dummy :options]}
                         :directory "/var/opt/gitlab/backups"
                         :url "localhost"
                         :prep-scripts {:prep-backup-script "rm -rf /var/opt/gitlab/backups/* && /bin/tar -czf /var/opt/gitlab/backups/gitlab_secrets /etc/ssh/ssh_host_* /etc/gitlab/gitlab-secrets.json /etc/ssh/authorized_keys/git && /usr/bin/gitlab-rake gitlab:backup:create SKIP=artifacts"
                                        :prep-restore-script "dummy-script"}
                         :post-ops {:remove-remote-backup {:days 21
                                                           :options (into [] (concat options-dup add-dup-op-delete-old))}
                                    :post-transport-script "dummy-script"}})

(deftest backup-files-duplicity
  (testing "backup files by using duplicity"
    (is (= ["#backup the files"
            "export PASSPHRASE= "
            "export TMPDIR=/var/opt/gitlab/backup-cache"
            "export AWS_ACCESS_KEY_ID=A1"
            "export AWS_SECRET_ACCESS_KEY=A1"
            "export S3_USE_SIGV4=True"
            "rm -rf /var/opt/gitlab/backups/* && /bin/tar -czf /var/opt/gitlab/backups/gitlab_secrets /etc/ssh/ssh_host_* /etc/gitlab/gitlab-secrets.json /etc/ssh/authorized_keys/git && /usr/bin/gitlab-rake gitlab:backup:create SKIP=artifacts"
            "/usr/bin/duplicity full --archive-dir /var/opt/gitlab/backup-cache --verbosity notice --s3-use-new-style --s3-european-buckets --encrypt-key=1A --sign-key=1A --asynchronous-upload --volsize=1500 --log-file /var/log/gitlab/duplicity.log /var/opt/gitlab/backups localhost"
            "/usr/bin/duplicity remove-older-than 21D --archive-dir /var/opt/gitlab/backup-cache --verbosity notice --s3-use-new-style --s3-european-buckets --encrypt-key=1A --sign-key=1A --log-file /var/log/gitlab/duplicity.log --force localhost"
            "unset AWS_ACCESS_KEY_ID"
            "unset AWS_SECRET_ACCESS_KEY"
            "unset S3_USE_SIGV4"
            "unset PASSPHRASE"
            "unset TMPDIR"
            ""]
           (sut/backup-files-duplicity dup-backup-element)))))
