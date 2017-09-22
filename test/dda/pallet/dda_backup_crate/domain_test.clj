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
                   :name "testname"
                   :root-dir s/Str
                   :subdir-to-save s/Str})

(def test-backup-config
  {:backup-name "test"
   :backup-user {:encrypted-passwd "WIwn6jIUt2Rbc"}
   :local-management {:gens-stored-on-source-system 2}
   :transport-management {}
   :backup-elements [test-backup-element]})

(def user-domain-config
  {:dda-backup test-backup-config})

(deftest user-domain-configuration-test
  (testing
   (is (= user-domain-config (sut/user-domain-configuration test-backup-config)))))

(deftest infra-backup-element-test
  (testing (is (= ))))
