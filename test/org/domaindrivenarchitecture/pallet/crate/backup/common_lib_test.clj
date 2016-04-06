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

(ns org.domaindrivenarchitecture.pallet.crate.backup.common-lib-test
  (:require
    [clojure.test :refer :all]
    [pallet.actions :as actions]
    [org.domaindrivenarchitecture.pallet.crate.backup.common-lib :as sut]
    ))

(deftest app-server
  (testing 
    "stop app server"
    (is (= ["#stop appserver"
            "service tomcat7 stop"
            ""]
           (sut/stop-app-server "tomcat7")))
    )  
  )

(deftest backup-file-name
  (testing 
    "backup-file-name"
    (is (= "meissa-server_owncloud_file_${timestamp}.dir"
           (sut/backup-file-name "meissa-server" "owncloud" :rsync)))
    )  
  )