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
    [org.domaindrivenarchitecture.pallet.crate.backup.restore-lib :as sut]
    ))

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
               :db-name "owncloud"})
           ))
    )
  )


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
             {
             :type :file-plain
             :name "name"
             :subdir-to-save "./"
             :root-dir "/var/www/owncloud"
             })
           ))
    )  
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
              :new-owner "tomcat7"})
           ))
    )  
  )