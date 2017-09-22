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

(ns dda.pallet.dda-backup-crate.domain.file-convention
  (:require
   [schema.core :as s]
   [dda.pallet.dda-backup-crate.domain.schema :as schema]))

(s/defn element-type-name
  [type :- schema/BackupElementType]
  (case type
    :file-compressed "file"
    :file-plain "file"
    :rsync "file"
    :mysql "mysql"
    :duplicity "file"))

(s/defn element-type-file-extension
  [type :- schema/BackupElementType]
  (case type
    :file-compressed "tgz"
    :file-plain "tar"
    :rsync "dir"
    :mysql "sql"
    :duplicity "dir"))

(s/defn backup-file-prefix-pattern :- s/Str
  ""
  [name :- s/Str
   type :- schema/BackupElementType]
  (str name "_" (element-type-name type)))

;TODO: this produces a string with the name doubled
(s/defn backup-file-prefix  :- s/Str
  ""
  [name :- s/Str
   type :- schema/BackupElementType]
  (str name "_" (backup-file-prefix-pattern name type)))

(s/defn backup-file-name
  ""
  [name :- s/Str
   type :- schema/BackupElementType]
  (str (backup-file-prefix name type)
       "_${timestamp}."
       (element-type-file-extension type)))
