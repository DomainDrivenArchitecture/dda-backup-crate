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

(ns dda.pallet.dda-backup-crate.infra.core.backup-element-test
  (:require
   [schema.core :as s]
   [clojure.test :refer :all]
   [pallet.actions :as actions]
   [pallet.build-actions :as build-actions]
   [dda.pallet.commons.plan-test-utils :as tu]
   [dda.pallet.dda-backup-crate.infra.core.backup-element :as sut]))

(def check-or-make-cache-folder "[[ -d /var/opt/gitblit/backup-cache ]] || mkdir /var/opt/gitblit/backup-cache")

(def remove-old-backups "rm -rf /var/opt/gitblit/backups/*")
(def tar-data "/bin/tar -czf /var/opt/gitblit/backups/current_backup /var/lib/tomcat7/webapps/gitblit")
(def tar-secrets  "/bin/tar -czf  /var/opt/gitblit/backups/current_secrets /etc/ssh/ssh_host_*")
(def prep-backup-script (str check-or-make-cache-folder " && " remove-old-backups " && " tar-data " && " tar-secrets))

(def prep-restore-script check-or-make-cache-folder)

(def unpack-data "/bin/tar -xzf /var/opt/gitblit/backups/current_backup --directory=/")
(def unpack-secrets "/bin/tar -xzf /var/opt/gitblit/backups/current_secrets --directory=/")
(def post-transport-script (str unpack-data " && " unpack-secrets))

(def min-options-dup [:archive-dir "/var/opt/gitblit/backup-cache"
                      :verbosity :notice
                      :s3-use-new-style true
                      :s3-european-buckets true
                      :encrypt-key=1A true
                      :sign-key=1A true])
(def logfile [:log-file "/var/log/gitblit/duplicity.log"])
(def add-dup-op-backup [:asynchronous-upload true
                        :volsize=1500 true])
(def add-dup-op-delete-old [:force true])

(def test-element {:type :duplicity
                   :tmp-dir "/var/opt/gitblit/backup-cache"
                   :trust-script-path " "
                   :priv-key-path " "
                   :pub-key-path " "
                   :passphrase " "
                   :aws-access-key-id "A1"
                   :aws-secret-access-key "A1"
                   :s3-use-sigv4 "True"
                   :action :full
                   :options {:backup-options (into [] (concat min-options-dup add-dup-op-backup logfile))
                             :restore-options (into [] (concat min-options-dup logfile))}
                   :directory "/var/opt/gitblit/backups"
                   :url "localhost"
                   :prep-scripts {:prep-backup-script prep-backup-script
                                  :prep-restore-script prep-restore-script}
                   :post-ops {:remove-remote-backup {:days 21
                                                     :options (into [] (concat min-options-dup logfile add-dup-op-delete-old))}
                              :post-transport-script post-transport-script}})

(deftest backup-element-dup-test
  "Testing "
  (testing
   (is (= test-element (s/validate sut/BackupElement test-element)))))
