(ns server
  (:require [clojure.core.async :refer [go]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [resource-response]]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [hiccup.page :refer [html5 include-css include-js]]
            [mtg]))

(load "mtg")

(defn imageurl [multiverseid]
  (format "https://gatherer.wizards.com/Handlers/Image.ashx?multiverseid=%d&type=card" multiverseid))

(defn cardimage [multiverseid]
  (html5 [:div.card {:_ "on click toggle .tapped"} [:img {:src (imageurl multiverseid)}]]))

; FOR NOW lands and _other_ permanents are separated in this simple way
(defn battlefield [player]
  (let [cards (mtg/zoneinfo player :battlefield [{:instance/card [:card/multiverseid {:card/type [:db/ident]}]}])
        lands (filter #(mtg/has-type? :land %) cards)
        permanents (remove #(mtg/has-type? :land %) cards)]
    (html5
     [:div.permanents
      (map #(cardimage (get-in % [:instance/card :card/multiverseid])) permanents)]
     [:div.lands
      (map #(cardimage (get-in % [:instance/card :card/multiverseid])) lands)])))

(comment
  (battlefield "Player1"))

(defn graveyard [player]
  (let [cards (mtg/zoneinfo player :graveyard [{:instance/card [:card/name]}])]
    (html5 [:div.list [:ul (map #(-> [:li (get-in % [:instance/card :card/name])]) cards)]])))

(comment
  (graveyard "Player1"))

(defn index []
  (let [player1 "Player1" player2 "Player2"]
    (html5 [:head
            [:title "Magic: the Gathering"]
            (include-css "css/style.css")
            (include-js "https://unpkg.com/hyperscript.org@0.9.12")
            (include-js "https://unpkg.com/htmx.org@1.9.12/dist/htmx.min.js")]
           [:body
            [:h1 {:class "button" :hx-get "/test"} "MTG"]
            [:div.mtg
             [:div.players "players"
              [:div.player player2]
              [:div.player player1]]
             [:div.graveyards "graveyards"
              [:div.graveyard (graveyard player2)]
              [:div.graveyard (graveyard player1)]]
             [:div.battlefields "battlefields"
              [:div.battlefield.opponent (battlefield player2)]
              [:div.battlefield (battlefield player1)]]
             [:div.stack "stack"]]])))

(defroutes app-routes
  (GET "/" [] (index))
  (GET "/res" [] (resource-response "index.html"))
  (GET "/test" [] (html5 [:h1 "You got it"]))
  (route/not-found (html5 [:h1 "Page not found"])))

(def app (wrap-resource app-routes "public"))

; runs in microthread, cannot bound twice. for dev
; redefine 'app to get functionality live while server is running!
(go (run-jetty app {:port 8080}))

; for prod
;(run-jetty app {:port 8080})