"use client";

import { useState } from "react";
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
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Log } from "@/utils/types";

interface LogsDialogProps {
  logs: Log[];
}

export function LogsDialog({ logs }: LogsDialogProps) {
  const [open, setOpen] = useState(false);

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline">View Logs</Button>
      </DialogTrigger>
      <DialogContent className="w-[85%] !max-w-none">
        <DialogHeader>
          <DialogTitle>System Logs</DialogTitle>
        </DialogHeader>
        <div className="overflow-y-auto max-h-[calc(80vh-120px)]">
          {logs.length !== 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[180px]">Time</TableHead>
                  <TableHead className="w-[150px]">Instance</TableHead>
                  <TableHead>Description</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {logs.map((log, index) => {
                  return (
                    <TableRow key={index}>
                      <TableCell className="font-mono text-xs">
                        <div>{log.timestamp}</div>
                      </TableCell>
                      <TableCell className="font-mono">
                        {log.instance}
                      </TableCell>
                      <TableCell>{log.description}</TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          ) : (
            <div className="text-center py-10 text-muted-foreground">
              <p className="text-lg">No logs</p>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
