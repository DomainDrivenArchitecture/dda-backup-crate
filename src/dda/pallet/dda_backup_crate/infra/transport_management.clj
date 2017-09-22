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

(ns dda.pallet.dda-backup-crate.infra.transport-management
  (:require
   [schema.core :as s]
   [selmer.parser :as selmer]
   [pallet.actions :as actions]
   [dda.pallet.dda-backup-crate.infra.schema :as schema]))

(s/defn init []
  (actions/package-source "duplicity"
                          :aptitude
                          {:url "ppa:duplicity-team/ppa"})
  (actions/package-manager :update))

(s/defn install []
  (actions/package "unzip")
  (actions/package "rng-tools")
  (actions/package "duplicity")
  (actions/package "python3")
  (actions/remote-directory
   "/tmp/boto-install"
   :action :create
   :url "https://github.com/boto/boto/archive/2.48.0.zip"
   :unpack :unzip
   :owner "root"
   :group "users"
   :mode "755")
  (actions/exec-script* "cd /tmp/boto-install/boto-2.48.0/ && /usr/bin/python setup.py install"))

(s/defn configure-duplicity [user :- s/Keyword
                             backup-script-path :- s/Str
                             backup-transport-folder :- s/Str
                             backup-restore-folder :- s/Str
                             transport-duplicity :- schema/TransportDuplicity]
  (let [user-name (name user)
        {:keys [target-s3 tmp-dir]} transport-duplicity]
    (actions/remote-file
     (str "/home/" user-name "/.credentials")
     :owner user-name
     :group user-name
     :mode "600"
     :content (selmer/render-file "credentials.template" transport-duplicity))
    (actions/remote-file
      (str "/home/" user-name "/.env")
      :owner user-name
      :group user-name
      :mode "644"
      :content (selmer/render-file "env.template" target-s3))
    (actions/remote-file
      (str backup-script-path "/duplicity_backup_transport.sh")
      :owner "root"
      :group user-name
      :mode "554"
      :content (selmer/render-file "duplicity_transport_backup.sh.template"
                                   {:backup-user-name user-name
                                    :backup-transport-folder backup-transport-folder
                                    :tmp-dir tmp-dir}))
    (actions/remote-file
      (str backup-script-path "/duplicity_restore_transport.sh")
      :owner "root"
      :group user-name
      :mode "554"
      :content (selmer/render-file "duplicity_transport_restore.sh.template"
                                   {:backup-user-name user-name
                                    :backup-restore-folder backup-restore-folder
                                    :tmp-dir tmp-dir}))))
