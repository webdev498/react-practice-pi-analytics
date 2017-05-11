// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import createReducer, {Action} from "redux-updeep";
import * as Actions from "../actions/RootActions";
import * as FetchActions from "../actions/FetchActions";
import ServiceAddressBundle from "../services/Types";
import u from "updeep";
import * as Queue from "../services/Queue";

const initialState = {
  loading: true
}

const root = createReducer(Actions.NAMESPACE, initialState, {
    [Actions.UNSORT_SERVICE_ADDRESS]: (state, action) => {
      let newState = u({
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
      u({value: undefined, loading: true, undo: {value: state.value, agent: action.payload.agent}}, state)
  })
;

export default root;

export const createFirm = (): Action => ({
  type: Actions.CREATE_FIRM,
  payload: {
    isCreateFirmDialogOpen: true
  }
});

export const closeFirmDialog = (): Action => ({
  type: Actions.CLOSE_FIRM_DIALOG,
  payload: {
    isCreateFirmDialogOpen: false
  }
})

export const unsortedServiceAddressFulfilled = (bundle: ServiceAddressBundle): Action => (dispatch: Dispatch) => {
  if (Queue.canPush()) {
    dispatch({type: FetchActions.PRE_FETCH_NEXT_UNSORTED_SERVICE_ADDRESS});
  }
  dispatch({type: Actions.UNSORTED_SERVICE_ADDRESS_FULFILLED, payload: {value: bundle, loading: false}});
};

export const unsortedServiceAddressPreFetched = (bundle: ServiceAddressBundle): Action => (dispatch: Dispatch) => {
  Queue.push(bundle);
  if (Queue.canPush()) {
    dispatch({type: FetchActions.PRE_FETCH_NEXT_UNSORTED_SERVICE_ADDRESS});
  }
}

export const unsortedServiceAddressFetchError = (): Action => ({
  type: Actions.UNSORTED_SERVICE_ADDRESS_FETCH_ERROR,
  payload: {
    loading: false
  }
});

export const serviceAddressAssignError = (): Action => ({
  type: Actions.SERVICE_ADDRESS_ASSIGNED,
  payload: {
    loading: false
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

export const getNextUnsortedServiceAddress = (): Action => (dispatch: Dispatch) => {
  var cached = Queue.pop();
  if (cached) {
    dispatch({type: Actions.UNSORTED_SERVICE_ADDRESS_FULFILLED, payload: {value: cached, loading: false}});
  } else {
    dispatch({type: Actions.START_FETCH, payload: {loading: true}});
    dispatch({type: FetchActions.FETCH_NEXT_UNSORTED_SERVICE_ADDRESS});
  }
};

export const skipServiceAddress = (): Action => (dispatch: Dispatch) => {
  dispatch({type: Actions.START_FETCH, payload: {value: undefined, loading: true}});
  dispatch({type: FetchActions.SKIP_SERVICE_ADDRESS, payload: {request: {delayMinutes: 1440}}});
};

export const dismissServiceAddress = (queueId: string, serviceAddressId: string): Action => (dispatch: Dispatch) => {
  dispatch({type: Actions.START_FETCH, payload: {value: undefined, loading: true}})
  dispatch(
    {
      type: FetchActions.SET_SERVICE_ADDRESS_AS_NON_LAW_FIRM,
      payload: {
        request: {
          unsortedServiceAddressQueueItemId: queueId,
          serviceAddressId: serviceAddressId
        }
      }
    });
};

export const unsortServiceAddress = (serviceAddressId: string): Action => (dispatch: Dispatch) => {
  dispatch({type: Actions.UNSORT_SERVICE_ADDRESS, payload: {serviceAddressId: serviceAddressId}});
  dispatch({type: FetchActions.UNSORT_SERVICE_ADDRESS, payload: {request: {serviceAddressId: serviceAddressId}}});
};

export const sortServiceAddress = (queueId: string, serviceAddressId: string,
                                   agent: Object): Action => (dispatch: Dispatch) => {
  dispatch({type: Actions.SORT_SERVICE_ADDRESS, payload: {agent: agent}});
  dispatch(
    {
      type: FetchActions.ASSIGN_SERVICE_ADDRESS,
      payload: {
        request: {
          unsortedServiceAddressQueueItemId: queueId,
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