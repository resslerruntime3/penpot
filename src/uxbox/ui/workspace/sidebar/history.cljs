;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2015-2016 Andrey Antukh <niwi@niwi.nz>
;; Copyright (c) 2015-2016 Juan de la Cruz <delacruzgarciajuan@gmail.com>

(ns uxbox.ui.workspace.sidebar.history
  (:require [sablono.core :as html :refer-macros [html]]
            [rum.core :as rum]
            [lentes.core :as l]
            [uxbox.locales :refer (tr)]
            [uxbox.router :as r]
            [uxbox.rstore :as rs]
            [uxbox.state :as st]
            [uxbox.shapes :as shapes]
            [uxbox.library :as library]
            [uxbox.util.datetime :as dt]
            [uxbox.util.data :refer (read-string)]
            [uxbox.data.workspace :as dw]
            [uxbox.data.pages :as dpg]
            [uxbox.ui.workspace.base :as wb]
            [uxbox.ui.icons :as i]
            [uxbox.ui.mixins :as mx]
            [uxbox.util.dom :as dom]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Lenses
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:const history-l
  (as-> (l/in [:workspace :history]) $
    (l/focus-atom $ st/state)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- history-toolbox-will-mount
  [own]
  (let [page @wb/page-l]
    (rs/emit! (dpg/fetch-page-history (:id page)))
    (add-watch wb/page-l ::key (fn [_ _ ov nv]
                                 (when (> (:version nv) (:version ov))
                                   (rs/emit! (dpg/fetch-page-history (:id nv))))))
    own))

(defn- history-toolbox-will-unmount
  [own]
  (rs/emit! (dpg/clean-page-history))
  (remove-watch wb/page-l ::key)
  own)

(defn- history-toolbox-transfer-state
  [oldown own]
  own)

(defn history-toolbox-render
  [own]
  (let [local (:rum/local own)
        page (rum/react wb/page-l)
        history (rum/react history-l)
        section (:section @local :main)
        close #(rs/emit! (dw/toggle-flag :document-history))
        main? (= section :main)
        pinned? (= section :pinned)
        show-main #(swap! local assoc :section :main)
        show-pinned #(swap! local assoc :section :pinned)]
    (html
     [:div.document-history.tool-window
      [:div.tool-window-bar
       [:div.tool-window-icon i/undo-history]
       [:span (tr "ds.document-history")]
       [:div.tool-window-close {:on-click close} i/close]]
      [:div.tool-window-content
       [:ul.history-tabs
        [:li {:on-click show-main
              :class (when main? "selected")}
         "History"]
        [:li {:on-click show-pinned
              :class (when pinned? "selected")}
         "Pinned"]]
       (if (= section :pinned)
         [:ul.history-content
          [:li.current
           [:span "Current version"]]
          [:li
           [:span "Version 02/02/2016 12:33h"]
           [:div.page-actions
            [:a i/pencil]
            [:a i/trash]]]])
       (if (= section :main)
         [:ul.history-content
          [:li.current
           [:div.pin-icon i/pin]
           [:span (str "Version " (:version page) " (current)")]]
          (for [item (:items history)]
            [:li {:key (str (:id item))}
             [:div.pin-icon i/pin]
             [:span (str "Version " (:version item)
                         " (" (dt/timeago (:created-at item)) ")")]])
          [:li
           [:a.btn-primary.btn-small "view more"]]])]])))

(def ^:static history-toolbox
  (mx/component
   {:render history-toolbox-render
    :name "document-history-toolbox"
    :will-mount history-toolbox-will-mount
    :will-unmount history-toolbox-will-unmount
    :transfer-state history-toolbox-transfer-state
    :mixins [mx/static rum/reactive (mx/local)]}))
