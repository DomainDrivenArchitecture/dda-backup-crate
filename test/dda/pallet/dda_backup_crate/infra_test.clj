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

(ns dda.pallet.dda-backup-crate.infra-test
  (:require
    [schema.core :as s]
    [clojure.test :refer :all]
    [pallet.build-actions :as build-actions]
    [dda.pallet.commons.plan-test-utils :as tu]
    [dda.pallet.dda-backup-crate.infra.schema :as schema]
    [dda.pallet.dda-backup-crate.infra :as sut]))

(deftest the-whole
  (testing
    "test plan creation"
      (is sut/with-backup)))

(def a-config
  {:backup-name "system-name"
   :backup-script-path ""
   :backup-transport-folder ""
   :backup-store-folder ""
   :backup-restore-folder ""
   :backup-user :dda-backup
   :local-management {:gens-stored-on-source-system 1}
   :transport-management {}
   :backup-elements []})

(deftest install
 (testing
   "test the default release definition"
   (is
     (.contains
       (tu/extract-nth-action-command
         (build-actions/build-actions
           build-actions/ubuntu-session
           (sut/install a-config))
         1)
       "Directory"))))