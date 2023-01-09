(ns simple-api.server
  (:require [io.pedestal.http.route :as route]
            [io.pedestal.http :as http]
            [io.pedestal.test :as test]
            [simple-api.database :as database]))

(defn create-task [request]
  (let [uuid (java.util.UUID/randomUUID)
        name (get-in request [:query-params :name])
        status (get-in request [:query-params :status])
        task {:id uuid :name name :status status}
        store (:store request)]
    (swap! store assoc uuid task)
    {:status 200 :body {:message "task created" :task task}}))

(defn assoc-store [context]
  (update context :request assoc :store database/store))

(def database-interceptor
  {:name :database-interceptor
   :enter assoc-store})

(defn get-task [request]
  {:status 200 :body @(:store request)})

(defn hello-function [request]
  {:status 200 :body (str "Hello World " (get-in request [:query-params :name] "everybody"))})

(defn delete-task [request]
  (let [store (:store request)
        task-id (get-in request [:path-params :id])
        task-id-uuid (java.util.UUID/fromString task-id)]
    (swap! store dissoc task-id-uuid)
    {:status 200 :body {:message "task removed"}}))

(defn update-task [request]
  (let [task-id (get-in request [:path-params :id])
        task-id-uuid (java.util.UUID/fromString task-id)
        name (get-in request [:query-params :name])
        status (get-in request [:query-params :status])
        task {:id task-id-uuid :name name :status status}
        store (:store request)]
    (swap! store assoc task-id-uuid task)
    {:status 200 :body {:message "task updated" :task task}}))

(def routes (route/expand-routes
              #{["/hello" :get hello-function :route-name :hello-world]
                ["/task" :post [database-interceptor create-task] :route-name :create-task]
                ["/task" :get [database-interceptor get-task] :route-name :get-task]
                ["/task/:id" :delete [database-interceptor delete-task] :route-name :delete-task]
                ["/task/:id" :patch [database-interceptor update-task] :route-name :update-task]}))

(def service-map {::http/routes routes
                  ::http/port   9999
                  ::http/type   :jetty
                  ::http/join?  false})

(defonce server (atom nil))
(defn start-server []
  (reset! server (http/start (http/create-server service-map))))

(defn test-request [verb url]
  (test/response-for (::http/service-fn @server) verb url)
  )

(defn stop-server []
  (http/stop @server))

(defn restart-server []
  (stop-server)
  (start-server))

;(start-server)
(start-server)
(println "Server started/restarted")
(println (test-request :get "/hello?name=matos"))
(println (test-request :post "/task?name=Run&status=pending"))
(println (test-request :post "/task?name=Running&status=done"))
(clojure.edn/read-string (:body (test-request :get "/task")))
;(println (test-request :delete "/task/05d463f1-063e-4a3e-b4bd-a04b40e40161"))
;(println (test-request :patch "/task/05d463f1-063e-4a3e-b4bd-a04b40e40161?name=tiago&status=failed"))
