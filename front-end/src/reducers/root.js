// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import createReducer from "redux-updeep";
import type {Action, Dispatch} from "redux";
import * as Actions from "../actions/RootActions";
import * as FetchActions from "../actions/FetchActions";
import type {ServiceAddressBundle} from "../services/Types";
import u from "updeep";
import sampleSize from "lodash/sampleSize";
import has from "lodash/has";
import last from "lodash/last";
import {store} from "../index";

const canPreFetch = () => !has(store.getState(), "root.serviceAddressesCache") ||
                          store.getState().root.serviceAddressesCache.length < 4;
const mapServiceAddressBundle = (bundle: ServiceAddressBundle) => {
  return u({suggestedAgents: u.map(agent => u({serviceAddresses: sampleSize(agent.serviceAddresses, 5)}, agent))}, bundle);
}

const initialState = {
  loading: true
}

const root = createReducer(Actions.NAMESPACE, initialState, {
  [Actions.UNSORT_SERVICE_ADDRESS]: (state, action) => {
    const newState = u({
                         value: {
                           suggestedAgents: u.map({
                                                    serviceAddresses: u.reject(
                                                      address => address.serviceAddressId == action.payload.serviceAddressId)
                                                  })
                         },
                         loading: true
                       }, state);
    return u({value: {suggestedAgents: u.reject(agent => agent.serviceAddresses.length == 0)}}, newState);
  },
  [Actions.SORT_SERVICE_ADDRESS]: (state, action) =>
    u({value: undefined, loading: true, undo: {value: state.value, agent: action.payload.agent}}, state),
  [Actions.UNSORTED_SERVICE_ADDRESS_PREFETCHED]: (state, action) =>
    u({
        serviceAddressesCache: () => state.serviceAddressesCache ? [action.payload].concat(state.serviceAddressesCache)
          : [action.payload]
      }, state),
  [Actions.CONSUME_CACHED_SERVICE_ADDRESS]: (state) =>
    u({
        value: u.constant(mapServiceAddressBundle(last(state.serviceAddressesCache))),
        serviceAddressesCache: state.serviceAddressesCache.slice(0, -1),
        loading: false
      }, state)
});

export default root;

export const createFirm = (): Action => ({
  type: Actions.CREATE_FIRM,
  payload: {
    isCreateFirmDialogOpen: true
  }
});

export const closeFirmDialog = (success: boolean): Action => {
  if (success) {
    return {
      type: Actions.LAW_FIRM_CREATED,
      payload: {
        isCreateFirmDialogOpen: false, message: {text: "Law firm successfully created.", error: false}, undo: undefined
      }
    };
  }
  else {
    return {type: Actions.LAW_FIRM_CANCELED, payload: {isCreateFirmDialogOpen: false}};
  }
};

export const showError = (errorMessage: string): Action => ({
  type: Actions.MESSAGE,
  payload: {
    message: {
      text: errorMessage,
      error: true
    }
  }
});

export const hideMessage = (): Action => ({
  type: Actions.MESSAGE,
  payload: {
    message: undefined
  }
});

export const globalFetchError = (): Action => ({
  type: Actions.MESSAGE,
  payload: {
    loading: false,
    message: {
      text: "Error fetching data",
      error: true
    }
  }
});

export const unsortedServiceAddressFetchError = (): Action => ({
  type: Actions.UNSORTED_SERVICE_ADDRESS_FETCH_ERROR,
  payload: {
    loading: false,
    value: null
  }
});

export const toggleApplicationsPanel = (previous: boolean): Action => ({
  type: Actions.TOGGLE_APPLICATIONS_PANEL,
  payload: {
    applicationsPanelOpen: !previous
  }
});

export const serviceAddressUnsorted = (): Action => ({
  type: Actions.SERVICE_ADDRESS_UNSORTED,
  payload: {
    loading: false
  }
});

export const undoServiceAddressSuccess = (): Action => ({
  type: Actions.UNDO_COMPLETED,
  payload: {
    loading: false,
    undo: undefined
  }
})

export const unsortedServiceAddressFulfilled = (bundle: ServiceAddressBundle): Action => (dispatch: Dispatch) => {
  if (canPreFetch()) {
    dispatch({type: FetchActions.PRE_FETCH_NEXT_UNSORTED_SERVICE_ADDRESS});
  }
  dispatch(
    {
      type: Actions.UNSORTED_SERVICE_ADDRESS_FULFILLED,
      payload: {value: mapServiceAddressBundle(bundle), loading: false}
    });
};

export const unsortedServiceAddressPreFetched = (bundle: ServiceAddressBundle): Action => (dispatch: Dispatch) => {
  dispatch({type: Actions.UNSORTED_SERVICE_ADDRESS_PREFETCHED, payload: bundle});
  if (canPreFetch()) {
    dispatch({type: FetchActions.PRE_FETCH_NEXT_UNSORTED_SERVICE_ADDRESS});
  }
}

export const getNextUnsortedServiceAddress = (): Action => (dispatch: Dispatch) => {
  if (store.getState().root.serviceAddressesCache && store.getState().root.serviceAddressesCache.length > 0) {
    dispatch({type: Actions.CONSUME_CACHED_SERVICE_ADDRESS});
    if (canPreFetch()) {
      dispatch({type: FetchActions.PRE_FETCH_NEXT_UNSORTED_SERVICE_ADDRESS});
    }
  } else {
    dispatch({type: Actions.START_FETCH, payload: {loading: true}});
    dispatch({type: FetchActions.FETCH_NEXT_UNSORTED_SERVICE_ADDRESS});
  }
};

export const dismissServiceAddress = (serviceAddressId: string, fetchActionType: string): Action => (dispatch: Dispatch) => {
  dispatch({type: Actions.START_FETCH, payload: {value: undefined, loading: true}})
  dispatch(
    {
      type: fetchActionType,
      payload: {request: {serviceAddressId: serviceAddressId}}
    });
};

export const unsortServiceAddress = (serviceAddressId: string): Action => (dispatch: Dispatch) => {
  dispatch({type: Actions.UNSORT_SERVICE_ADDRESS, payload: {serviceAddressId: serviceAddressId}});
  dispatch({type: FetchActions.UNSORT_SERVICE_ADDRESS, payload: {request: {serviceAddressId: serviceAddressId}}});
};

export const sortServiceAddress = (serviceAddressId: string, agent: Object): Action => (dispatch: Dispatch) => {
  dispatch({type: Actions.SORT_SERVICE_ADDRESS, payload: {agent: agent}});
  dispatch(
    {
      type: FetchActions.ASSIGN_SERVICE_ADDRESS,
      payload: {
        request: {
          serviceAddressId: serviceAddressId,
          lawFirmId: agent.lawFirm.lawFirmId
        }
      }
    });
}

export const undoServiceAddress = (serviceAddressId: string) => (dispatch: Dispatch) => {
  dispatch({type: Actions.START_FETCH, payload: {loading: true}});
  dispatch({type: FetchActions.UNDO_SERVICE_ADDRESS, payload: {request: {serviceAddressId: serviceAddressId}}});
}