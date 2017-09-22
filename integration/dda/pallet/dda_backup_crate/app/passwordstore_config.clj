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

(ns dda.pallet.dda-backup-crate.app.passwordstore-config
  (:require
   [dda.config.commons.user-env :as user-env]
   [dda.pallet.commons.passwordstore-adapter :as adapter]))

(def ssh-pub-key (user-env/read-ssh-pub-key-to-config))
(def os-user
  {:encrypted-password "kpwejjj0r04u09rg90rfj"
   :authorized-keys [ssh-pub-key]})

(def ssh-domain-config
  {:backup-name "ssh"
   :backup-user os-user
   :local-management {:gens-stored-on-source-system 3}
   :elements [{:type :file-compressed
               :name "ssh"
               :root-dir "/etc/"
               :subdir-to-save "ssh"}]})

(def duplicity-domain-config
  {:backup-name "duplicity"
   :backup-user os-user
   :local-management {:gens-stored-on-source-system 1}
   :transport-management  {:duplicity-push
                           {:public-key (adapter/get-secret "meissa/system/backup-meissa.pub")
                            :private-key (adapter/get-secret "meissa/system/backup-meissa.sec")
                            :passphrase (adapter/get-secret-wo-newline "meissa/system/backup-meissa.passphrase")
                            :target-s3 {:aws-access-key-id (adapter/get-secret-wo-newline "meissa/system/aws/backup.key.id")
                                        :aws-secret-access-key (adapter/get-secret-wo-newline "meissa/system/aws/backup.key.secret")
                                        :bucket-name "meissa-backup"}}}
   :backup-elements
   [{:type :file-compressed
     :name "ssh"
     :root-dir "/etc/"
     :subdir-to-save "ssh"}]})
