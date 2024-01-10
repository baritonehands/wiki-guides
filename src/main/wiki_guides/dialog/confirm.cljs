(ns wiki-guides.dialog.confirm
  (:require [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :refer [modal-panel box h-box v-box p title button]]))

(defn prompt-button [& {:as args}]
  (->> args
       (mapcat identity)
       (into [button :parts {:wrapper {:style {:flex "1 0 auto"}}}])))

(defn view [{:keys      [text confirm-label cancel-label on-confirm on-cancel]
             title-text :title}]
  [modal-panel
   :backdrop-on-click on-cancel
   :style {:max-width "100vw"}
   :child
   [v-box
    :gap "5px"
    :children
    [[title :level :level2 :label title-text]
     [p {:style {:width     "95%"
                 :min-width "325px"}} text]
     [h-box
      :gap "10px"
      :children
      [[box
        :size "1"
        :child
        [prompt-button
         :label confirm-label
         :class "btn-primary btn-lg btn-block"
         :on-click on-confirm]]
       [box
        :size "1"
        :child
        [prompt-button
         :label cancel-label
         :class "btn-lg btn-block"
         :on-click on-cancel]]]]]]])
