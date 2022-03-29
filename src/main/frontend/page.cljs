(ns frontend.page
  (:require [rum.core :as rum]
            [frontend.state :as state]
            [frontend.ui :as ui]
            [frontend.components.sidebar :as sidebar]
            [frontend.handler.search :as search-handler]
            [frontend.handler.notification :as notification]
            [frontend.handler.plugin :as plugin-handler]))

(rum/defc route-view
  [view route-match]
  (view route-match))

(defn- setup-fns!
  []
  (try
    (comp
      (ui/setup-active-keystroke!)
      (ui/setup-patch-ios-visual-viewport-state!))
    (catch js/Error _e
      nil)))

(rum/defc helpful-default-error-screen
  "This screen is displayed when the UI has crashed hard. It provides the user
  with basic troubleshooting steps to get them back to a working state. This
  component is purposefully stupid simple as it needs to render under any number
  of broken conditions"
  []
  ;; This layout emulates most of sidebar/sidebar
  [:div#main-container.cp__sidebar-main-layout.flex-1.flex
   [:div.#app-container
    [:div#left-container
     [:div#main-container.cp__sidebar-main-layout.flex-1.flex
      [:div#main-content-container.scrollbar-spacing.w-full.flex.justify-center
       [:div.cp__sidebar-main-content
        [:div.ls-center
         [:div.text-center (ui/icon "bug" {:style {:font-size ui/icon-size}})]
         [:div.font-bold.text-center
          "Sorry. Something went wrong!"]
         [:div.mt-2.mb-2 "Logseq is having a problem. To try to get it back to a
         working state, please try the following safe steps in order:"]
         [:div
          [:div.flex.flex-row.justify-between.align-items.mb-2
           [:div.flex.flex-col.items-start
            [:div.text-xs "STEP 1"]
            [:div [:span.font-bold "Reload"] " the app"]]
           [:div (ui/icon "command") (ui/icon "letter-r")]]
          [:div.flex.flex-row.justify-between.align-items.mb-2
           [:div.flex.flex-col.items-start
            [:div.text-xs "STEP 2"]
            [:div [:span.font-bold "Relaunch"] " the app"]
            [:div.text-xs "Quit the app and then reopen it."]]
           [:div (ui/icon "command") (ui/icon "letter-q")]]
          [:div.flex.flex-row.justify-between.align-items.mb-2
           [:div.flex.flex-col.items-start
            [:div.text-xs "STEP 3"]
            [:div [:span.font-bold "Clear"] " local storage"]]
           (ui/button "Try"
                      :small? true
                      :on-click (fn []
                                  (.clear js/localStorage)
                                  (notification/show! "Cleared!" :success)))]
          [:div.flex.flex-row.justify-between.align-items.mb-2
           [:div.flex.flex-col.items-start
            [:div.text-xs "STEP 4"]
            [:div [:span.font-bold "Rebuild"] " search index"]]
           (ui/button "Try"
                      :small? true
                      :on-click (fn []
                                  (search-handler/rebuild-indices! true)))]]
         [:div
          [:p "If you think you have experienced data loss, check for backup files under
          the folder logseq/bak/."]
          [:p "If these troubleshooting steps have not solved your problem, please "
           [:a.underline
            {:href "https://github.com/logseq/logseq/issues"}
            "open an issue."]]]]]]]]]
   (ui/notification)])

(rum/defc current-page < rum/reactive
  {:did-mount    (fn [state]
                   (state/set-root-component! (:rum/react-component state))
                   (state/setup-electron-updater!)
                   (ui/inject-document-devices-envs!)
                   (ui/inject-dynamic-style-node!)
                   (plugin-handler/host-mounted!)
                   (assoc state ::teardown (setup-fns!) ))
   :will-unmount (fn [state]
                   (when-let [teardown (::teardown state)]
                     (teardown)))}
  []
  (when-let [route-match (state/sub :route-match)]
    (let [route-name (get-in route-match [:data :name])]
      (when-let [view (:view (:data route-match))]
        (ui/catch-error
         (helpful-default-error-screen)
         (if (= :draw route-name)
           (view route-match)
           (sidebar/sidebar
            route-match
            (view route-match))))))))

        ;; FIXME: disable for now
        ;; (let [route-name (get-in route-match [:data :name])
        ;;       no-animate? (contains? #{:repos :repo-add :file}
        ;;                              route-name)]
        ;;   (when-let [view (:view (:data route-match))]
        ;;     (sidebar/sidebar
        ;;      route-match
        ;;      (if no-animate?
        ;;        (route-view view route-match)
        ;;        (ui/transition-group
        ;;         {:class-name "router-wrapper"}
        ;;         (ui/css-transition
        ;;          {:class-names "pageChange"
        ;;           :key route-name
        ;;           :timeout {:enter 300
        ;;                     :exit 200}}
        ;;          (route-view view route-match)))))))
