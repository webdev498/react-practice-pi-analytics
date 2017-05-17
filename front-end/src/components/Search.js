// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import {Content, Icon, Spinner, Textfield} from "react-mdl";
import React from "react";
import AddressesList from "./AddressesList";
import Rx from "rxjs";
import "rxjs/add/operator/map";
import "rxjs/add/operator/debounceTime";

type
SearchProps = {
  agents: Array < Object >,
  loading: boolean,
  query: string,
  serviceAddress: Object,
  queueId: string,
  onSearch: (query: string) => void,
  onSortServiceAddress: (queueId: string, serviceAddressId: string, address: Object) => void,
  onUnsortServiceAddress: (serviceAddressId: string) => void
}

class Search extends React.Component {
  props: SearchProps;
  bindSearchInputChange: function;

  constructor(props: SearchProps) {
    super(props)
    this.bindSearchInputChange = this.bindSearchInputChange.bind(this);
  }

  bindSearchInputChange(input: any) {
    if (!input) {
      return;
    }
    Rx.Observable.fromEvent(input.inputRef, "keyup")
      .map(event => event.target.value)
      .debounceTime(200)
      .subscribe(this.props.onSearch);
  }

  render() {
    let content = ""
    if (!this.props.loading && this.props.query && this.props.query.length > 0 && this.props.agents
        && this.props.agents.length > 0) {
      content =
        <AddressesList agents={this.props.agents} queueId={this.props.queueId} serviceAddress={this.props.serviceAddress}
                       onSortServiceAddress={this.props.onSortServiceAddress}
                       onUnsortServiceAddress={this.props.onUnsortServiceAddress}/>
    } else if (!this.props.loading && this.props.query && this.props.query.length > 0) {
      content = <h4 className="voffset32" style={{color: "#bbb"}}>No matching law firms</h4>
    } else if (this.props.loading) {
      content = <Spinner singleColor/>
    }

    return (
      <div>
        <div style={{textAlign: "center", margin: "16px"}}>
          <Icon style={{verticalAlign: "sub"}} name="search"/>
          <Textfield ref={this.bindSearchInputChange} style={{marginLeft: "8px"}} label="Search law firm by name" />
        </div>
        <Content className={"center"}>
          {content}
        </Content>
      </div>
    )
  }
}

export default Search