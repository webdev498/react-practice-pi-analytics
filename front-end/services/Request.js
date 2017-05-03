// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import type from "redux";
import {Observable} from "rxjs/Observable";
import "rxjs/add/operator/retryWhen";
import "rxjs/add/operator/delay";
import "rxjs/add/observable/dom/ajax";
import Authentication from "./Authentication";

export
type
Request = {
  url: string,
  method: "POST" | "GET" | "PUT" | "PATCH" | "DELETE",
  body? : Object
}

export const Methods = {
  POST: "POST",
  GET: "GET",
  PUT: "PUT",
  PATCH: "PATCH",
  DELETE: "DELETE",
}

export const fetch = (request: Request): Observable => {
  return Observable.ajax(decorateRequest(request))
    .catch(error => {
      if (error.status === 401) {
        window.location = "login.jsp#agents";
      } else {
        throw error;
      }
    })
}

/**
 * Takes an Rx.DOM.ajax() settings object and returns a clone of the settings with Citation Eagle
 * additions applied to it.
 *
 * See https://github.com/Reactive-Extensions/RxJS-DOM/blob/master/doc/operators/ajax.md#rxdomajaxsettings
 * for setting options.
 */
const decorateRequest = (request: Request): Object => {
  if (Authentication.user) {
    request.body.requested_by = Authentication.user;
  }
  return Object.assign({}, request, {
    responseType: "json",
    crossDomain: true,
    body: request.body ? JSON.stringify(request.body) : undefined
  });
}
