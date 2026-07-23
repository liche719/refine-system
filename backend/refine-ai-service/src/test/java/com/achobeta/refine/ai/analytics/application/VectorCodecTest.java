package com.achobeta.refine.ai.analytics.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class VectorCodecTest {
    private final VectorCodec codec = new VectorCodec();

    @Test
    void serializesRemoteVectorsAndCalculatesCosine() {
        double[] vector = {0.1D, 0.2D, 0.3D};
        assertThat(codec.parse(codec.serialize(vector))).containsExactly(vector);
        assertThat(codec.cosine(vector, vector)).isCloseTo(1D, offset(0.000001D));
    }
}
