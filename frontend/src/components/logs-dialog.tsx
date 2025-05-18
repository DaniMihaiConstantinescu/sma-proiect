"use client";

import { useState, useMemo } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { InstanceType, Log } from "@/utils/types";

interface LogsDialogProps {
  logs: Log[];
}

export function LogsDialog({ logs }: LogsDialogProps) {
  const [open, setOpen] = useState(false);
  const [filter, setFilter] = useState<string>("ALL");

  const instanceTypeLabels = {
    [InstanceType.WEBSOCKET_SERVER]: "WebSocket Server",
    [InstanceType.GATEWAY]: "Gateway",
    [InstanceType.REVERSE_PROXY]: "Reverse Proxy",
    [InstanceType.LOAD_BALANCER]: "Load Balancer",
    [InstanceType.NODE]: "Node",
  };

  const filteredLogs = useMemo(() => {
    if (filter === "ALL") return logs;
    return logs.filter((log) => InstanceType[log.instanceType] === filter);
  }, [logs, filter]);

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline">View Logs</Button>
      </DialogTrigger>
      <DialogContent className="w-[85%] !max-w-none">
        <DialogHeader>
          <DialogTitle>System Logs</DialogTitle>
        </DialogHeader>

        <div className="mb-2">
          <Select value={filter} onValueChange={setFilter}>
            <SelectTrigger className="w-64">
              <SelectValue placeholder="Filter by instance type" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All</SelectItem>
              {Object.entries(InstanceType)
                .filter(([key]) => isNaN(Number(key)))
                .map(([key, value]) => (
                  <SelectItem key={key} value={value.toString()}>
                    {instanceTypeLabels[value as InstanceType] ?? key}
                  </SelectItem>
                ))}
            </SelectContent>
          </Select>
        </div>

        <div className="overflow-y-auto max-h-[calc(80vh-160px)]">
          <Table>
            {filteredLogs.length === 0 && <TableCaption>No logs</TableCaption>}
            <TableHeader>
              <TableRow>
                <TableHead className="w-[180px]">Time</TableHead>
                <TableHead className="w-[150px]">Instance</TableHead>
                <TableHead>Description</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredLogs.map((log, index) => (
                <TableRow key={index}>
                  <TableCell className="font-mono text-xs">
                    {log.timestamp}
                  </TableCell>
                  <TableCell className="font-mono">{log.instance}</TableCell>
                  <TableCell>{log.description}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </DialogContent>
    </Dialog>
  );
}
