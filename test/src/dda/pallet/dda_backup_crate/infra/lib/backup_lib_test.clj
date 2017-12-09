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

(deftest backup-files-tar
  (testing
   "backup files as compressed archive"
    (is (= ["#backup the files"
            "cd /var/lib/liferay/data/"
            "tar cvzf /var/backups/transport-outgoing/portal_prod_file_${timestamp}.tgz document_library"
            "chown dda-backup:dda-backup /var/backups/transport-outgoing/portal_prod_file_${timestamp}.tgz"
            ""]
           (sut/backup-files-tar
            "portal"
            "/var/backups/transport-outgoing"
            "dda-backup"
            {:name "prod"
             :type :file-compressed
             :backup-file-name "portal_prod_file_${timestamp}.tgz"
             :root-dir "/var/lib/liferay/data/"
             :subdir-to-save "document_library"}))))
  (testing
   "backup files as uncompressed archive"
    (is (= ["#backup the files"
            "cd /var/lib/liferay/data/"
            "tar cvf /var/backups/transport-outgoing/portal_prod_file_${timestamp}.tar document_library"
            "chown dda-backup:dda-backup /var/backups/transport-outgoing/portal_prod_file_${timestamp}.tar"
            ""]
           (sut/backup-files-tar
            "portal"
            "/var/backups/transport-outgoing"
            "dda-backup"
            {:name "prod"
             :type :file-plain
             :backup-file-name "portal_prod_file_${timestamp}.tar"
             :root-dir "/var/lib/liferay/data/"
             :subdir-to-save "document_library"})))))

(deftest backup-files-rsync
  (testing
    (is (= ["#backup the files"
            "cd /var/lib/liferay/data/"
            "rsync -Aax document_library /var/backups/transport-outgoing/portal_prod_file"
            ""]
           (sut/backup-files-rsync
            "portal"
            "/var/backups/transport-outgoing"
            {:name "prod"
             :type :rsync
             :backup-file-name "portal_prod_file"
             :root-dir "/var/lib/liferay/data/"
             :subdir-to-save "document_library"})))))

(deftest backup-mysql
  (testing
    (is (= ["#backup db"
            "mysqldump --no-create-db=true -h localhost -u db-user -pdb-passwd db-name > /var/backups/transport-outgoing/portal_prod_file_${timestamp}.sql"
            "chown dda-backup:dda-backup /var/backups/transport-outgoing/portal_prod_file_${timestamp}.sql"
            ""]
           (sut/backup-mysql
            "portal"
            "/var/backups/transport-outgoing"
            "dda-backup"
            {:name "prod"
             :type :mysql
             :backup-file-name "portal_prod_file_${timestamp}.sql"
             :db-user-name "db-user"
             :db-user-passwd "db-passwd"
             :db-name "db-name"})))))
