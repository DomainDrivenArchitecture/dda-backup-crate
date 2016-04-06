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
    [org.domaindrivenarchitecture.pallet.crate.backup.restore-lib-0-2 :as sut]
    ))

(deftest restore-mysql
  (testing 
    "restore for owncloud"
    (is (= ["mysql -hlocalhost -uowncloud -powncloud-db-pwd -e \"drop database owncloud\";"
            "mysql -hlocalhost -uowncloud -powncloud-db-pwd -e \"create database owncloud\";";
            "mysql -hlocalhost -uowncloud -powncloud-db-pwd owncloud < ${most_recent_sql_dump}"
            ""]
           (sut/restore-mysql 
                :db-user "owncloud" 
                :db-pass "owncloud-db-pwd" 
                :db-name "owncloud")
           ))
    )  
  (testing 
    "restore for liferay"
    (is (= ["mysql -hlocalhost -uprod -pintermediate-db-pwd -e \"drop database lportal\";"
           "mysql -hlocalhost -uprod -pintermediate-db-pwd -e \"create database lportal character set utf8\";"
           "mysql -hlocalhost -uprod -pintermediate-db-pwd lportal < output2.sql"
           ""]
           (sut/restore-mysql 
                :db-user "prod" 
                :db-pass "intermediate-db-pwd" 
                :db-name "lportal"
                :dump-filename "output2.sql"
                :create-options "character set utf8")
           ))
    )  
  )


(deftest restore-tar
  (testing 
    "restore for owncloud"
    (is (= ["rm -r /var/www/owncloud/*"
            "tar --same-owner --same-permissions -xf ${most_recent_file_dump} -C /var/www/owncloud"
            ""]
           (sut/restore-tar
             :restore-target-dir "/var/www/owncloud"
             :file-type :file-plain)
           ))
    )  
  (testing 
    "restore for liferay"
    (is (= ["rm -r /var/lib/liferay/data/*"
             "tar -xzf ${most_recent_file_dump} -C /var/lib/liferay/data"
             "chown -R tomcat7:tomcat7 /var/lib/liferay/data"
             ""]
           (sut/restore-tar
             :restore-target-dir "/var/lib/liferay/data"
             :file-type :file-compressed
             :new-owner "tomcat7")
           ))
    )  
  )