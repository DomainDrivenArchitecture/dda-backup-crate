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

(ns dda.pallet.dda-backup-crate.infra.local-management
  (:require
   [schema.core :as s]
   [pallet.actions :as actions]
   [dda.pallet.dda-backup-crate.infra.schema :as schema]))

(s/defn create-backup-directory
  "create the backup user with directory structure."
  [user :- s/Keyword
   backup-transport-folder :- s/Str
   backup-store-folder :- s/Str
   backup-restore-folder :- s/Str]
  (let [backup-user-name (name user)]
    (actions/directory backup-transport-folder
                       :action :create
                       :owner backup-user-name
                       :group backup-user-name)
    (actions/directory backup-store-folder
                       :action :create
                       :owner backup-user-name
                       :group backup-user-name)
    (actions/directory backup-restore-folder
                       :action :create
                       :owner backup-user-name
                       :group backup-user-name)))

(s/defn create-script-environment
  "create directory for backup scripts."
  [script-path :- s/Str]
  (actions/directory
   script-path
   :action :create
   :owner "root"
   :group "root"))
