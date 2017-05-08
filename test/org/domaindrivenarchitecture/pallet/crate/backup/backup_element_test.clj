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

(ns org.domaindrivenarchitecture.pallet.crate.backup.backup-element-test
  (:require
   [schema.core :as s]
   [clojure.test :refer :all]
   [pallet.actions :as actions]
   [pallet.build-actions :as build-actions]
   [org.domaindrivenarchitecture.pallet.commons.plan-test-utils :as tu]
   [org.domaindrivenarchitecture.pallet.crate.backup :as backup]
   [org.domaindrivenarchitecture.pallet.crate.backup.backup-element :as sut]))

(def min-options-dup [:archive-dir "/var/opt/gitblit/backup-cache"
                      :verbosity :notice
                      :s3-use-new-style true
                      :s3-european-buckets true])

(def add-dup-op-backup [:asynchronous-upload true
                        :volsize=1500 true
                        :log-file "/var/log/gitblit/duplicity.log"])

(def add-dup-op-delete-old [:log-file "/var/log/gitblit/duplicity.log"
                            :force true])

(def test-element {:type :duplicity
                   :tmp-dir "/var/opt/gitblit/backup-cache"
                   :trust-script-path " "
                   :priv-key-path " "
                   :pub-key-path " "
                   :passphrase " "
                   :aws-access-key-id " "
                   :aws-secret-access-key " "
                   :s3-use-sigv4 "True"
                   :action :full
                   :options {:backup-options (into [] (concat min-options-dup add-dup-op-backup))
                             :restore-options min-options-dup}
                   :directory "/var/opt/gitblit/backups"
                   :url " "
                   :prep-scripts {:prep-backup-script " "
                                  :prep-restore-script " "}
                   :post-ops {:remove-remote-backup {:days 21
                                                     :options (into [] (concat min-options-dup add-dup-op-delete-old))}
                              :post-transport-script " "}})

(deftest backup-element-dup-test
  "Testing "
  (testing
   (is (= test-element (s/validate sut/BackupElement test-element)))))
