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


(ns dda.pallet.dda-backup-crate.infra.lib.restore-lib
  (require
   [schema.core :as s]
   [dda.pallet.dda-backup-crate.infra.schema :as schema]
   [selmer.parser :as selmer]))

(defn render-file
  [file map]
  (selmer.util/without-escaping
     (selmer/render-file file map)))

(s/defn restore-dump-name
  "Get the newest file for restore."
  [backup-element :- schema/BackupElement]
  (let [{:keys [name type-name]} backup-element]
    (str "most_recent_" name "_" type-name "_dump")))

(s/defn get-restore-dump
  "Get the newest file for restore."
  [backup-element :- schema/BackupElement]
  (let [{:keys [backup-file-prefix-pattern]} backup-element]
    [(str (restore-dump-name backup-element)
          "=$(ls -d -t1 "
          backup-file-prefix-pattern
          " | head -n1)")]))

(s/defn echo-restore-dump
  "Echo used file for restore."
  [element :- schema/BackupElement]
  [(str "echo \"$"
        (restore-dump-name element)
        "\"")])

(defn provide-restore-dumps
  "Provide the most recent files for restore."
  [elements]
  (into
   []
   (concat
    ["# Get the dumps"]
    (mapcat get-restore-dump elements)
    [""
     "echo \"using this inputs:\""]
    (mapcat echo-restore-dump elements)
    [""])))

(s/defn restore-head-element
  [element :- schema/BackupElement]
  (str "[ \"$" (restore-dump-name element) "\" ]"))

(defn restore-head-script
  [elements]
  [(str "if "
        (clojure.string/join
         " && "
         (map restore-head-element elements))
        "; then")
   "echo \"starting restore\""])

(def restore-tail
  ["echo \"finished restore successfull, pls. start the appserver.\""
   "fi"
   ""])

(defn restore-element-type
  [backup-element]
  (-> backup-element :type))

(defmulti restore-element restore-element-type)

(defmethod restore-element :file-compressed
  [element]
  (render-file "restore_templates/restore_file.template"
                      {:backup-path (clojure.string/join " " (:backup-path element))
                       :tar-own-options (if (contains? element :new-owner)
                                          nil
                                          "--same-owner --same-permissions")
                       :tar-compress-option "-xzf"
                       :chown (if (contains? element :new-owner)
                                (str "chown -R " (:new-owner element)
                                      ":" (:new-owner element)
                                      " " (clojure.string/join " " (:backup-path element)))
                                nil)
                       :restore-dump-name (str "{" (restore-dump-name element) "}")}))

(defmethod restore-element :file-plain
  [element]
  (render-file "restore_templates/restore_file.template"
                      {:backup-path (clojure.string/join " " (:backup-path element))
                       :tar-own-options (if (contains? element :new-owner)
                                          nil
                                          "--same-owner --same-permissions")
                       :tar-compress-option "-xf"
                       :chown (if (contains? element :new-owner)
                                [(str "chown -R " (:new-owner element)
                                      ":" (:new-owner element)
                                      " " (clojure.string/join " " (:backup-path element)))]
                                nil)
                       :restore-dump-name (str "{" (restore-dump-name element) "}")}))

(defmethod restore-element :mysql
  [element]
  (let [{:keys [db-user-name db-user-passwd db-name db-create-options]
         :or   {db-create-options ""}} element]
    (render-file "restore_templates/restore_mysql.template"
                        {:db-pre-processing (clojure.string/join "\n" (:db-pre-processing element))
                         :db-user-name db-user-name
                         :db-user-passwd db-user-passwd
                         :db-name db-name
                         :db-create-options db-create-options
                         :restore-dump-name (str "{" (restore-dump-name element) "}")
                         :db-post-processing (clojure.string/join "\n" (:db-post-processing element))})))

;TODO What is the argument restore-target-dir? It is not present in any configuration.
(defn restore-rsync
  [& {:keys [restore-target-dir
             new-owner]
      :or {dump-filename "${most_recent_file_dump}"}}]
  (into []
        (concat
         [(str "rm -r " restore-target-dir "/*")
          (str "rsync -Aax"
               " ${most_recent_file_dump}/"
               restore-target-dir
               "/ "
               restore-target-dir)]
         (if new-owner
           [(str "chown -R " new-owner ":" new-owner " " restore-target-dir)]
           [])
         [""])))

