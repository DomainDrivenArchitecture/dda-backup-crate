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

(ns dda.pallet.dda-backup-crate.app-test
  (:require
   [schema.core :as s]
   [clojure.test :refer :all]
   [dda.pallet.commons.secret :as secret]
   [dda.pallet.commons.external-config :as ext-config]
   [dda.pallet.dda-backup-crate.app :as sut]))

(defn test-backup-snakeoil
  []
  (ext-config/parse-config "test-backup-snakeoil.edn"))

(deftest schame-validity-test
  (testing
   (is (s/validate sut/BackupConfig (test-backup-snakeoil)))
   (is (s/validate sut/BackupConfigResolved
          (secret/resolve-secrets (test-backup-snakeoil) sut/BackupConfig)))))


(deftest secret-resolving-test
  (testing
   (is (= ""
          (secret/resolve-secrets (test-backup-snakeoil) sut/BackupConfig)))))
