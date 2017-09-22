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

(ns dda.pallet.dda-backup-crate.infra.lib.backup-lib-test
  (:require
   [clojure.test :refer :all]
   [schema.core :as s]
   ;[dda.pallet.dda-backup-crate.infra.core.backup-element-test :as backup-element]
   [dda.pallet.dda-backup-crate.infra.lib.backup-lib :as sut]))

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

; (deftest backup-files-duplicity
;   (testing "backup files by using duplicity"
;     (is (= ["#backup the files"
;             "export PASSPHRASE= "
;             "export TMPDIR=/var/opt/gitblit/backup-cache"
;             "export AWS_ACCESS_KEY_ID=A1"
;             "export AWS_SECRET_ACCESS_KEY=A1"
;             "export S3_USE_SIGV4=True"
;             backup-element/prep-backup-script
;             "/usr/bin/duplicity full --gpg-binary gpg2 --archive-dir /var/opt/gitblit/backup-cache --verbosity notice --s3-use-new-style --s3-european-buckets --encrypt-key=1A --sign-key=1A --asynchronous-upload --volsize=1500 --log-file /var/log/gitblit/duplicity.log /var/opt/gitblit/backups localhost"
;             "/usr/bin/duplicity remove-older-than 21D --gpg-binary gpg2 --archive-dir /var/opt/gitblit/backup-cache --verbosity notice --s3-use-new-style --s3-european-buckets --encrypt-key=1A --sign-key=1A --log-file /var/log/gitblit/duplicity.log --force localhost"
;             "unset AWS_ACCESS_KEY_ID"
;             "unset AWS_SECRET_ACCESS_KEY"
;             "unset S3_USE_SIGV4"
;             "unset PASSPHRASE"
;             "unset TMPDIR"
;             ""]
;            (sut/backup-files-duplicity backup-element/test-element)))))
