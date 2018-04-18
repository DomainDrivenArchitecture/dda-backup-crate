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

(ns dda.pallet.dda-backup-crate.infra.lib.backup-lib
  (require
    [selmer.parser :as selmer]))

(defn backup-element-type
  [& {:keys [backup-element]}]
  (-> backup-element :type))

(defmulti backup-element backup-element-type)

(defmethod backup-element :file-compressed
  [& {:keys [backup-element backup-name backup-transport-folder user-name]}]
  (selmer/render-file "backup_templates/backup_file.template" {:backup-transport-folder backup-transport-folder
                                                               :backup-file-name (:backup-file-name backup-element)
                                                               :backup-path (:backup-path backup-element)
                                                               :user-name user-name
                                                               :tar-options "cvzf"}))

(defmethod backup-element :file-plain
  [& {:keys [backup-element backup-name backup-transport-folder user-name]}]
  (selmer/render-file "backup_templates/backup_file.template" {:backup-transport-folder backup-transport-folder
                                                               :backup-file-name       (:backup-file-name backup-element)
                                                               :backup-path (:backup-path backup-element)
                                                               :user-name               user-name
                                                               :tar-options             "cvf"}))

(defmethod backup-element :mysql
  [& {:keys [backup-element backup-transport-folder user-name]}]
  (selmer/render-file "backup_templates/backup_mysql.template" {:backup-file-name        (:backup-file-name backup-element)
                                                                :backup-transport-folder backup-transport-folder
                                                                :user-name               user-name
                                                                :db-name                 (:db-name backup-element)
                                                                :db-user-name            (:db-user-name backup-element)
                                                                :db-user-passwd          (:db-user-passwd backup-element)
                                                                }))

(defmethod backup-element :rsync
  [& {:keys [backup-element backup-transport-folder]}]
  (selmer/render-file "backup_templates/backup_rsync.template" {:backup-transport-folder backup-transport-folder
                                                                :backup-file-name        (:backup-file-name backup-element)
                                                                :backup-path             (:backup-path backup-element)}))