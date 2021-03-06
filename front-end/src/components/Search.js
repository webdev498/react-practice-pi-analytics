// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import {Content, Icon, Spinner, Textfield} from "react-mdl"
import React from "react"
import AddressesList from "./AddressesList"
import Rx from "rxjs"
import "rxjs/add/operator/map"
import "rxjs/add/operator/debounceTime"
import { findDOMNode } from 'react-dom'

type
SearchProps = {
  agents: Array < Object >,
  loading: boolean,
  query: string,
  serviceAddress: Object,
  onSearch: (query: string, countryCode: string) => void,
  onSortServiceAddress: (serviceAddressId: string, address: Object) => void,
  onUnsortServiceAddress: (serviceAddressId: string) => void
}

class Search extends React.Component {
  props: SearchProps
  bindSearchInputChange: () => void
  searchInput: any

  constructor(props: SearchProps) {
    super(props)
    this.bindSearchInputChange = this.bindSearchInputChange.bind(this)
  }

  componentDidMount() {
    if (this.props.query) {
      const node: any = findDOMNode(this.searchInput);
      if (node) {
        node.MaterialTextfield.change(this.props.query.substr(0));
      }
    }
  }

  bindSearchInputChange(input: any) {
    if (!input) {
      return
    }
    this.searchInput = input
    Rx.Observable.fromEvent(this.searchInput.inputRef, "keyup")
      .map(event => event.target.value)
      .debounceTime(200)
      .subscribe(query => this.props.onSearch(query, this.props.serviceAddress.country))
  }

  render() {
    let content = ""
    if (!this.props.loading && this.props.query && this.props.query.length > 0 && this.props.agents
        && this.props.agents.length > 0) {
      content =
        <AddressesList agents={this.props.agents} serviceAddress={this.props.serviceAddress}
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
          <Textfield ref={this.bindSearchInputChange} style={{marginLeft: "8px"}}
                     label="Search law firm by name"/>
        </div>
        <div className={"center"}>
          {content}
        </div>
      </div>
    )
  }
}

export default Search
