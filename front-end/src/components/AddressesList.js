// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import React from "react"
import {DataTable, IconButton, TableHeader} from "react-mdl"
import "../styles/main.scss"
import type {Agent, ServiceAddress} from "../services/Types"
import {OuterUrls} from "../services/Urls"

type
AddressesListProps = {
  serviceAddress: Object,
  agents: Array < Agent >,
  queueId: string,
  onSortServiceAddress: (queueId: string, serviceAddressId: string, agent: Agent) => void,
  onUnsortServiceAddress: (serviceAddressId: string) => void
}

class AddressesList extends React.Component {
  props: AddressesListProps
  state: Object

  constructor(props: AddressesListProps) {
    super(props)
    this.state = {selected: ""}
  }

  getSelectedClassName(agentIndex: number, addressIndex: number) {
    return this.state.selected === agentIndex + ":" + addressIndex ? "selected" : ""
  }

  select(agentIndex: number, addressIndex: number, disabled: boolean) {
    if (!disabled) {
      this.setState(prevState => ({
        selected: agentIndex + ":" + addressIndex,
      }))
    }
  }

  unselect(agentIndex: number, addressIndex: number) {
    if (this.state.selected === agentIndex + ":" + addressIndex) {
      this.setState(prevState => ({selected: ""}))
    }
  }

  renderLawFirmId(agent: Agent): React.Element<any> {
    if (agent.lawFirm) {
      const lawFirmId = agent.lawFirm ? agent.lawFirm.lawFirmId : 0
      const href = OuterUrls.firmInfo + lawFirmId + "#agents"
      const website = this.renderWebsite(agent)
      return (
        <span>
          {lawFirmId}&nbsp;<a href={href} target="_blank"><IconButton accent name="open_in_new"/></a>{website}
        </span>
      )
    }
    return <span style={{color: "#bbb"}}>Non law firm</span>
  }

  renderEntity(agent: Agent): React.Element<any> {
    if (agent.lawFirm) {
      return <a className="entity" href="#" onClick={(event: Event) => {
        event.preventDefault()
        this.props.onSortServiceAddress(this.props.queueId, this.props.serviceAddress.serviceAddressId, agent)
      }}>{agent.lawFirm.name}</a>
    }
    return <span className="entity">{agent.nonLawFirm ? agent.nonLawFirm.name : ""}</span>
  }

  renderAddressLine(agentIndex: number, index: number, value: ServiceAddress, testValue: string): React.Element<any> {
    let result = value.name + " " + value.address
    const upperCase = result.toUpperCase()
    if (testValue && testValue.length > 0 && value.address) {
      testValue
        .toUpperCase()
        .replace(/[;:.,]/g, " ")
        .replace(/  /g, " ")
        .split(" ")
        .sort((a, b) => b.length - a.length)
        .filter(item => item.length > 1 && upperCase.indexOf(item) >= 0)
        .forEach(item => {
          [item, item.toLowerCase(), item.substr(0, 1) + item.substr(1).toLowerCase()].forEach(test => {
            result = result.split(test).join("<span class='highlighted'>" + test + "</span>")
          });
        });
    }
    return (
      <div onMouseOver={() => this.select(agentIndex, index, agentIndex < 0)}
           onMouseLeave={() => this.unselect(agentIndex, index)}
           className={"address-line " + this.getSelectedClassName(agentIndex, index)}><span
        dangerouslySetInnerHTML={{__html: value.address ? result : "N/A"}}/>
      </div>
    );
  }

  renderServiceAddressWithInclusions(agentIndex: number, agent: Agent, testValue: string): Array<any> {
    return agent.serviceAddresses.map(
      (value, index) => this.renderAddressLine(agent.serviceAddresses.length > 1 ? agentIndex : -1,
                                               index, value, testValue))
  }

  renderWebsite(agent: Agent): ?React.Element<any> {
    if (agent.lawFirm && agent.lawFirm.websiteUrl) {
      return <a href={agent.lawFirm.websiteUrl} target="_blank"><IconButton name="public" accent/></a>
    }
    return null
  }

  renderEntityId(agentIndex: number, agent: Agent): Array<any> {
    return agent.serviceAddresses
      .map((address, index) => <div onMouseOver={() => this.select(agentIndex, index, agent.serviceAddresses.length < 2)}
                                    onMouseLeave={() => this.unselect(agentIndex, index)}
                                    className={
                                      "address-line " + this.getSelectedClassName(agentIndex, index)
                                    }>{address.serviceAddressId}</div>)
  }

  renderActions(agentIndex: number, agent: Agent): Array<any> {
    return agent.serviceAddresses.map(
      (address, index) => <div onMouseOver={() => this.select(agentIndex, index, agent.serviceAddresses.length < 2)}
                               onMouseLeave={() => this.unselect(agentIndex, index)}
                               className={this.getSelectedClassName(agentIndex, index)}
                               style={{paddingLeft: "2px", marginBottom: "3px"}}>
        <a href="#">
          <IconButton name="cancel" accent onClick={() => {
            this.props.onUnsortServiceAddress(address.serviceAddressId)
          }}/>
        </a>
      </div>)
  }

  mapRows() {
    return this.props.agents.map((agent: Agent, index: number) => ({
      key: index,
      lawFirmId: this.renderLawFirmId(agent),
      entity: this.renderEntity(agent),
      serviceAddress: this.renderServiceAddressWithInclusions(index, agent, this.props.serviceAddress.name + " "
                                                                            + this.props.serviceAddress.address),
      serviceAddressId: this.renderEntityId(index, agent),
      actions: this.renderActions(index, agent),
    }))
  }

  render() {
    return (
      <DataTable className="addresses-list" style={{width: "100%", tableLayout: "fixed"}} rowKeyColumn="key"
                 rows={this.mapRows()}>
        <TableHeader style={{width: "15%"}} name="lawFirmId">Law Firm</TableHeader>
        <TableHeader style={{width: "21%"}} name="entity">Firm</TableHeader>
        <TableHeader style={{paddingLeft: "26px", width: "53%"}} name="serviceAddress">Service Addresses</TableHeader>
        <TableHeader style={{paddingLeft: "26px", width: "6%"}} name="serviceAddressId">Entity ID</TableHeader>
        <TableHeader style={{width: "5%"}} name="actions">Re-sort</TableHeader>
      </DataTable>
    )
  }
}

export default AddressesList
