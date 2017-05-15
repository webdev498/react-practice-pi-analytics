
declare module "redux-updeep" {
  declare type Action = { type: string, payload: any }
  declare type State = any;
  declare type Reducer<S, A> = (state: S, action: A) => S;
  declare function createReducer(name: string, state: State, handlers: any): Reducer<any, any>
  declare var exports: createReducer
}

