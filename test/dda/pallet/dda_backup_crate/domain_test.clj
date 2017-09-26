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

(ns dda.pallet.dda-backup-crate.domain-test
  (:require
   [schema.core :as s]
   [clojure.test :refer :all]
   [dda.pallet.dda-backup-crate.domain :as sut]))

(def test-backup-element {:type :file-compressed
                          :name "ssh"
                          :root-dir "/etc/"
                          :subdir-to-save "ssh"})

(def test-domain-backup-config
  {:backup-name "test"
   :backup-user {:encrypted-password "WIwn6jIUt2Rbc"}
   :local-management {:gens-stored-on-source-system 2}
   :transport-management {:ssh-pull true}
   :backup-elements [test-backup-element]})

(def user-domain-config
  {:dda-backup {:encrypted-password "WIwn6jIUt2Rbc"}})

(def test-infra-backup-element (merge test-backup-element
                                      {:backup-file-prefix-pattern "ssh_file*",
                                       :backup-file-name "ssh_file_${timestamp}.tgz"
                                       :type-name "file"}))

(def test-backup-infra-config
  {:backup-name "test"
   :backup-script-path "/usr/local/lib/dda-backup/"
   :backup-transport-folder "/var/backups/transport-outgoing"
   :backup-store-folder "/var/backups/store"
   :backup-restore-folder "/var/backups/restore"
   :backup-user :dda-backup
   :local-management {:gens-stored-on-source-system 2}
   :transport-management {:ssh-pull true}
   :backup-elements [test-infra-backup-element]})

(def infra-result-config
  {:dda-backup test-backup-infra-config})

(deftest user-domain-configuration-test
  (testing
   (is (= user-domain-config (sut/user-domain-configuration test-domain-backup-config)))))

(deftest infra-backup-element-test
  (testing (is (= test-infra-backup-element (sut/infra-backup-element test-backup-element)))))

(deftest infra-config-test
  (testing (is (= test-backup-infra-config (sut/infra-config test-domain-backup-config)))))

(deftest infra-configuration-test
  (testing (is (= infra-result-config (sut/infra-configuration test-domain-backup-config)))))
