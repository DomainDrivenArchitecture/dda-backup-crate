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
   "/tmp/"
   :action :create
   :url "https://github.com/boto/boto/archive/2.48.0.zip"
   :unpack :unzip
   :owner "root"
   :group "users"
   :mode "755")
  (actions/exec-script* "cd /tmp/boto-2.48.0/ && /usr/bin/python setup.py install"))

(s/defn configure-duplicity [user :- s/Keyword
                             transport-duplicity :- schema/TransportDuplicity]
  (let [user-name (name user)]
    (actions/remote-file
     (str "/home/" user-name "/credentials")
     :owner user-name
     :group user-name
     :mode "600"
     :content (selmer/render-file "credentials.template" transport-duplicity))))
