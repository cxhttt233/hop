package org.apache.hop.www.modern;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import org.apache.hop.core.Result;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.workflow.*;
import org.apache.hop.workflow.action.*;
import org.apache.hop.workflow.actions.start.ActionStart;
import org.apache.hop.workflow.engines.local.LocalWorkflowEngine;
import org.junit.jupiter.api.Test;

class WorkflowLifecycleProductProbeTest {
  @Test void productOwnedAsyncHolderPreservesEngineLifecycle() throws Exception {
    CountDownLatch entered=new CountDownLatch(1), release=new CountDownLatch(1);
    WorkflowMeta meta=new WorkflowMeta();
    meta.setName("product-lifecycle-probe");
    ActionMeta start=new ActionMeta(new ActionStart("START"));
    ActionMeta block=new ActionMeta(new BlockingAction(entered,release));
    meta.addAction(start); meta.addAction(block);
    meta.addWorkflowHop(new WorkflowHopMeta(start,block));
    try (Holder holder=new Holder(meta)) {
      holder.start();
      assertTrue(entered.await(30,TimeUnit.SECONDS));
      assertEquals(new Status(true,false,false,"Running"),holder.status());
      holder.stop();
      assertEquals(new Status(true,true,false,"Halting"),holder.status());
      release.countDown();
      holder.awaitTerminal();
      assertEquals(new Status(false,true,false,"Stopped"),holder.status());
      assertTrue(holder.result().isStopped());
      assertEquals(0,holder.result().getNrErrors());
    } finally { release.countDown(); }
  }

  private static final class Holder implements AutoCloseable {
    final LocalWorkflowEngine engine;
    final ExecutorService executor=Executors.newSingleThreadExecutor();
    Future<?> future;
    Holder(WorkflowMeta meta){engine=new LocalWorkflowEngine(meta);}
    void start(){future=executor.submit(engine::startExecution);}
    void stop(){engine.stopExecution();}
    Status status(){return new Status(engine.isActive(),engine.isStopped(),engine.isFinished(),engine.getStatusDescription());}
    void awaitTerminal() throws Exception {future.get(60,TimeUnit.SECONDS);}
    Result result(){return engine.getResult();}
    public void close(){engine.stopExecution();executor.shutdownNow();}
  }
  private record Status(boolean active,boolean stopped,boolean finished,String description){}

  private static final class BlockingAction extends ActionBase implements IAction {
    final CountDownLatch entered,release;
    BlockingAction(CountDownLatch entered,CountDownLatch release){super("blocking","","BlockingAction");this.entered=entered;this.release=release;}
    public Result execute(Result previous,int nr) throws HopException {
      entered.countDown();
      try { if(!release.await(60,TimeUnit.SECONDS)) throw new HopException("probe timeout"); }
      catch(InterruptedException e){Thread.currentThread().interrupt();throw new HopException(e);}
      Result r=previous==null?new Result():previous.clone();
      r.setResult(true);r.setNrErrors(0);r.setStopped(false);return r;
    }
  }
}
