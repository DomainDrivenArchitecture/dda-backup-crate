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
    [pallet.actions :as actions]
    [org.domaindrivenarchitecture.pallet.crate.backup.backup-lib :as sut]
    ))

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
              :db-name "lportal"})))
    ))

(deftest backup-files-tar 
  (testing 
    "backup files as compressed archive"
    (is (= [ "#backup the files" 
             "cd /var/lib/liferay/data/"
             "tar cvzf /home/dataBackupSource/transport-outgoing/portal_prod_file_${timestamp}.tgz document_library"
             "chown dataBackupSource:dataBackupSource /home/dataBackupSource/transport-outgoing/portal_prod_file_${timestamp}.tgz"
             ""]
           (sut/backup-files-tar
             "portal"
             {:type :file-compressed
             :name "prod"
             :root-dir "/var/lib/liferay/data/"
             :subdir-to-save "document_library"})))
    )
  (testing
    "backup files as uncompressed archive"
    (is (= [ "#backup the files" 
             "cd /var/lib/liferay/data/"
             "tar cvf /home/dataBackupSource/transport-outgoing/portal_prod_file_${timestamp}.tar document_library"
             "chown dataBackupSource:dataBackupSource /home/dataBackupSource/transport-outgoing/portal_prod_file_${timestamp}.tar"
             ""]
           (sut/backup-files-tar 
             "portal"
             {:type :file-plain
             :name "prod"
             :root-dir "/var/lib/liferay/data/"
             :subdir-to-save "document_library"})))
    )
  )