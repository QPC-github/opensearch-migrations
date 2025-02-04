package org.opensearch.migrations.replay.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import lombok.extern.log4j.Log4j2;
import org.opensearch.migrations.replay.AggregatedRawResponse;

import java.util.function.Consumer;

@Log4j2
public class BacksideHttpWatcherHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final AggregatedRawResponse.Builder aggregatedRawResponseBuilder;
    private boolean doneReadingRequest; // later, when connections are reused, switch this to a counter?
    Consumer<AggregatedRawResponse> responseCallback;

    public BacksideHttpWatcherHandler(AggregatedRawResponse.Builder aggregatedRawResponseBuilder) {

        this.aggregatedRawResponseBuilder = aggregatedRawResponseBuilder;
        doneReadingRequest = false;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        doneReadingRequest = true;
        if (this.responseCallback != null) {
            this.responseCallback.accept(aggregatedRawResponseBuilder.build());
        }
        super.channelReadComplete(ctx);
    }

    public void addCallback(Consumer<AggregatedRawResponse> callback) {
        if (doneReadingRequest) {
            callback.accept(aggregatedRawResponseBuilder.build());
        } else {
            this.responseCallback = callback;
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.trace("inactive channel - closing");
        super.channelInactive(ctx);
    }

}
