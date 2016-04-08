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

(ns org.domaindrivenarchitecture.pallet.crate.backup.backup-element
  (:require
    [schema.core :as s]
    ))

(def ElementType
  "The backup elements"
  (s/enum :mysql :file-compressed :file-plain :rsync))

(def BackupElement
  "The backup elements"
  (s/conditional 
    #(= (:type %) :mysql)
    {:type ElementType
     :name s/Str
     :db-user-name s/Str 
     :db-user-passwd s/Str
     :db-name s/Str
     (s/optional-key :db-create-options) s/Str
     (s/optional-key :db-pre-processing) [s/Str]
     (s/optional-key :db-post-processing) [s/Str]}
    #(= (:type %) :file-compressed)
    {:type ElementType
     :name s/Str
     :root-dir s/Str
     :subdir-to-save s/Str
     (s/optional-key :new-owner) s/Str}
    ))

(s/defn element-type-name
  [type :- ElementType]
  (case type
    :file-compressed "file"
    :file-plain "file"
    :rsync "file"
    :mysql "mysql")
  )

(s/defn element-type-file-extension
  [type :- ElementType]
  (case type
    :file-compressed "tgz"
    :file-plain "tar"
    :rsync "dir"
    :mysql "sql")
  )

(s/defn backup-file-prefix-pattern :- s/Str
  ""
  [element :- BackupElement]
  (str (get-in element [:name]) "_" (element-type-name (get-in element [:type])))
  )

(s/defn backup-file-prefix  :- s/Str
  ""
  [backup-name :- s/Str
   element :- BackupElement]
  (str backup-name "_" (backup-file-prefix-pattern element)))

(s/defn backup-file-name
  ""
  [backup-name :- s/Str
   element :- BackupElement]
  (str (backup-file-prefix backup-name element) 
       "_${timestamp}." 
       (element-type-file-extension (get-in element [:type])))
  )