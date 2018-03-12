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
    (is (= ["#backup the specified files and directories"
            "tar cvzf /var/backups/transport-outgoing/portal_prod_file_${timestamp}.tgz /var/lib/liferay/data/ /var/lib/httpd/www/"
            "chown dda-backup:dda-backup /var/backups/transport-outgoing/portal_prod_file_${timestamp}.tgz"]
           (clojure.string/split-lines (sut/backup-element
                                         :backup-name "portal"
                                         :backup-transport-folder "/var/backups/transport-outgoing"
                                         :user-name "dda-backup"
                                         :backup-element {:name             "prod"
                                                          :type             :file-compressed
                                                          :backup-file-name "portal_prod_file_${timestamp}.tgz"
                                                          :backup-path      ["/var/lib/liferay/data/" "/var/lib/httpd/www/"]
                                                          })))))
  (testing
    "backup files as compressed archive"
    (is (= ["#backup the specified files and directories"
            "tar cvf /var/backups/transport-outgoing/portal_prod_file_${timestamp}.tgz /var/lib/liferay/data/ /var/lib/httpd/www/"
            "chown dda-backup:dda-backup /var/backups/transport-outgoing/portal_prod_file_${timestamp}.tgz"]
           (clojure.string/split-lines (sut/backup-element
                                         :backup-name "portal"
                                         :backup-transport-folder "/var/backups/transport-outgoing"
                                         :user-name "dda-backup"
                                         :backup-element {:name             "prod"
                                                          :type             :file-plain
                                                          :backup-file-name "portal_prod_file_${timestamp}.tgz"
                                                          :backup-path      ["/var/lib/liferay/data/" "/var/lib/httpd/www/"]
                                                          }))))))

(deftest backup-files-rsync
  (testing
    (is (= ["#backup the files"
            "rsync -Aax /var/lib/liferay/data/ /var/lib/httpd/www/ /var/backups/transport-outgoing/portal_prod_file_${timestamp}.tgz"
            ]
           (clojure.string/split-lines (sut/backup-element
                                         :backup-name "portal"
                                         :backup-transport-folder "/var/backups/transport-outgoing"
                                         :user-name "dda-backup"
                                         :backup-element {:name             "prod"
                                                          :type             :rsync
                                                          :backup-file-name "portal_prod_file_${timestamp}.tgz"
                                                          :backup-path      ["/var/lib/liferay/data/" "/var/lib/httpd/www/"]
                                                          }))))))



(deftest backup-mysql
  (testing
    (is (= ["#backup db"
            "mysqldump --no-create-db=true -h localhost -u db-user -pdb-passwd db-name > /var/backups/transport-outgoing/portal_prod_file_${timestamp}.sql"
            "chown dda-backup:dda-backup /var/backups/transport-outgoing/portal_prod_file_${timestamp}.sql"]
           (clojure.string/split-lines (sut/backup-element
                                         :backup-name "portal"
                                         :backup-transport-folder "/var/backups/transport-outgoing"
                                         :user-name "dda-backup"
                                         :backup-element {:name             "prod"
                                                          :type             :mysql
                                                          :backup-file-name "portal_prod_file_${timestamp}.sql"
                                                          :db-user-name "db-user"
                                                          :db-user-passwd "db-passwd"
                                                          :db-name "db-name"
                                                          }))))))
